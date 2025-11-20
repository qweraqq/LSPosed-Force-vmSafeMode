package com.xx;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import android.content.pm.ApplicationInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "[LSPosed-vmSafeMode] ";
    private static final int FLAG_VM_SAFE_MODE = 1<<14;
    private static final Set<String> TARGET_PACKAGES = new HashSet<>(Arrays.asList(
            "com.icbc",
            "com.bankcomm.Bankcomm",
            "com.bankcomm",
            "com.bankcomm.maidanba"
    ));
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"android".equals(lpparam.packageName)) return;
        XposedBridge.log(TAG + "Hook android to force enable vmSafeMode");

        Class<?> packageInfoUtilsClazz = XposedHelpers.findClassIfExists(
                "com.android.server.pm.parsing.PackageInfoUtils",
                lpparam.classLoader);

        if (packageInfoUtilsClazz != null) {
            for (Method method : packageInfoUtilsClazz.getDeclaredMethods()) {
                if (method.getReturnType() == ApplicationInfo.class) {
                    XposedBridge.log(TAG + "Hook <Class>" + packageInfoUtilsClazz.getCanonicalName() + " <Method>" + method.getName());

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            if (param.args == null) return;
                            ApplicationInfo appInfo = (ApplicationInfo) param.getResult();
                            if (TARGET_PACKAGES.contains(appInfo.packageName) && (appInfo.flags & FLAG_VM_SAFE_MODE) == 0) {
                                appInfo.flags |= FLAG_VM_SAFE_MODE;
                                // XposedBridge.log("VmSafeMode: Force-enabled (Runtime) for " + appInfo.packageName);
                                param.setResult(appInfo);
                            }

                        }
                    });

                }
            }
        }


        Class<?> processListClass = XposedHelpers.findClassIfExists(
                "com.android.server.am.ProcessList",
                lpparam.classLoader
        );
        if (processListClass != null) {
            for (Method method: processListClass.getDeclaredMethods()) {
                if (method.getName().equals("startProcessLocked")){
                    XposedBridge.log(TAG + "Hook <Class>" + packageInfoUtilsClazz.getCanonicalName() + " <Method>" + method.getName());
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args == null) return;

                            ApplicationInfo appInfo = null;
                            for (Object arg : param.args) {
                                if (arg instanceof ApplicationInfo) {
                                    appInfo = (ApplicationInfo) arg;
                                    break;
                                }
                            }
                            if (appInfo != null && TARGET_PACKAGES.contains(appInfo.packageName) && (appInfo.flags & FLAG_VM_SAFE_MODE) == 0) {
                                appInfo.flags |= FLAG_VM_SAFE_MODE;
                                // XposedBridge.log("VmSafeMode: Force-enabled (Runtime) for " + appInfo.packageName);
                            }
                        }
                    });
                }
            }
        }




    }
}
