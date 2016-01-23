package com.fkzhang.qqunrecalled;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

/**
 * Created by fkzhang on 1/20/2016.
 */
public class QQUnrecalledHook {
    private Set<String> RevokedUids;
    private Class<?> MessageRecordFactory;

    public QQUnrecalledHook(){
        RevokedUids = new HashSet<>();
    }

    public void hook(ClassLoader loader) {
        try {
            hookQQMessageFacade(loader);
        } catch (Throwable t) {
//            XposedBridge.log(t);
        }

    }


    protected void hookQQMessageFacade(final ClassLoader loader) {
        findAndHookMethod("com.tencent.mobileqq.app.message.QQMessageFacade", loader,
                "a", ArrayList.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param)
                            throws Throwable {
                        ArrayList list = (ArrayList) param.args[0];
                        if (list == null || list.isEmpty())
                            return;

                        Object obj = list.get(0);
                        param.setResult(null); // prevent call

                        if (MessageRecordFactory == null)
                            MessageRecordFactory = findClass("com.tencent.mobileqq.service.message.MessageRecordFactory", loader);

                        setMessageTip(param.thisObject, obj);

                    }
                });
    }

    private void setMessageTip(Object QQMessageFacade, Object revokeMsgInfo) {
        String tostring = revokeMsgInfo.toString();
        String msguid = extractValue("msguid", tostring);
        if(RevokedUids.contains(msguid)){
            return;
        }
        RevokedUids.add(msguid);

        String selfUin = (String) getObjectField(revokeMsgInfo, "a");
        String friendUin = (String) getObjectField(revokeMsgInfo, "b");

        String msg = "对方尝试撤回一条消息 （已阻止）";
        long time = System.currentTimeMillis() / 1000;
        int istroop = Integer.parseInt(extractValue("istroop", tostring));
        long msgUid = Long.parseLong(msguid) + new Random().nextInt();
        long shmsgseq = Long.parseLong(extractValue("shmsgseq", tostring)) + 1;

        callMethod(QQMessageFacade, "a", createMessageTip(selfUin, friendUin, msgUid, shmsgseq, time,
                msg, istroop), selfUin);
    }

    private List createMessageTip(String selfUin, String friendUin, long msgUid, long shmsgseq,
                                  long time, String msg, int istroop) {
        int msgtype = -2031; // MessageRecord.MSG_TYPE_REVOKE_GRAY_TIPS
        Object messageRecord = callStaticMethod(MessageRecordFactory, "a", msgtype);
        callMethod(messageRecord, "init", selfUin, friendUin, friendUin, msg, time, msgtype,
                istroop, time);
        setObjectField(messageRecord, "msgUid", msgUid);
        setObjectField(messageRecord, "shmsgseq", shmsgseq);
        setObjectField(messageRecord, "isread", true);

        List<Object> list = new ArrayList<>();
        list.add(messageRecord);

        return list;
    }

    private String extractValue(String tag, String text) {
        int idx = text.indexOf(tag);
        String startText = text.substring(idx, text.length());
        startText = startText.substring(startText.indexOf("=") + 1, startText.length()).trim();
        int idx2 = startText.indexOf(",");
        if (idx2 < 0) {
            idx2 = startText.indexOf(" ");
        }
        return startText.substring(0, idx2).trim();
    }


}
