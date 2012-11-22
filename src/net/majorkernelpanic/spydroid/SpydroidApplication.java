package net.majorkernelpanic.spydroid;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import static org.acra.ReportField.*;

@ReportsCrashes(formKey = "dGhWbUlacEV6X0hlS2xqcmhyYzNrWlE6MQ", customReportContent = { APP_VERSION_NAME, PHONE_MODEL, BRAND, PRODUCT, ANDROID_VERSION, STACK_TRACE, USER_APP_START_DATE, USER_CRASH_DATE, LOGCAT, DEVICE_FEATURES, SHARED_PREFERENCES })
public class SpydroidApplication extends android.app.Application {
	@Override
	  public void onCreate() {
	      // The following line triggers the initialization of ACRA
	      ACRA.init(this);
	      super.onCreate();
	  }
}
