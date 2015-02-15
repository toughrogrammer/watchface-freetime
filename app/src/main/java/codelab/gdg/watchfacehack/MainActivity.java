package codelab.gdg.watchfacehack;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        Intent viewIntent = new Intent(getApplicationContext(), MainActivity.class);
        viewIntent.putExtra("Title", 001);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, viewIntent, 0);
        NotificationCompat.Builder notificationbuilder = new NotificationCompat.Builder(getApplicationContext()).setSmallIcon(R.drawable.icon2)
                .setContentTitle("R U Free ?")
                .setContentText("누가누가 잉여인가??")
                .setContentIntent(viewPendingIntent)
                .setColor(253)
                .setWhen(2000);

        NotificationManagerCompat notificationmanager = NotificationManagerCompat.from(getApplicationContext());
        notificationmanager.notify(1, notificationbuilder.build());
    }
}
