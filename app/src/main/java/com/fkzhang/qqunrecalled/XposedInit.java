package com.fkzhang.qqunrecalled;

import android.util.SparseArray;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by fkzhang on 1/16/2016.
 */
public class XposedInit implements IXposedHookLoadPackage {
    private SparseArray<QQUnrecalledHook> mQQHooks;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.contains("com.tencent.mobileqq"))
            return;

//        XposedBridge.log("Loaded app: " + loadPackageParam.packageName);

        QQUnrecalledHook hooks = getHooks(loadPackageParam.appInfo.uid);
        if (hooks != null)
            hooks.hook(loadPackageParam.classLoader);
    }

    private QQUnrecalledHook getHooks(int uid) {
        if (mQQHooks == null) {
            mQQHooks = new SparseArray<>();
        }
        if (mQQHooks.indexOfKey(uid) != -1) {
            return mQQHooks.get(uid);
        }

        mQQHooks.put(uid, new QQUnrecalledHook());

        return mQQHooks.get(uid);
    }

}
