package me.leops.hashtalk.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by leops on 23/03/2016.
 */
public class AuthService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new Authenticator(this).getIBinder();
    }
}
