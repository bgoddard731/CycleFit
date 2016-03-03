package com.example.bgodd_000.locationtrack;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

public class simList extends AppCompatActivity {

    private ArrayList<smallRouteSummary> sim_routes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_list);
        sim_routes = Globals.sL;
        initializeView();
    }

    public void simRouteListClick(View v){
        TextView temp = (TextView) v;
        String name = temp.getTag().toString();
        Intent prevIntent = new Intent(this, PrevRouteActivity.class);
        prevIntent.putExtra("routeName",name);
        startActivity(prevIntent);
    }

    public void initializeView(){
        for (smallRouteSummary s : sim_routes) {
            //LinearLayout.LayoutParams
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            TextView entry = new TextView(this);
            entry.setLayoutParams(lp);
            Date temp = s.end;
            String entry_text = temp.toString();
            entry_text += String.format("\nDistance: %.2fm\nTime: %.2fs\nCalories Burned: %.2f cal",s.totalDistance, s.elapsedTime, s.calorieBurn);
            entry.setText(entry_text);
            entry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            entry.setTag(s.end.getTime());
            entry.setPadding(0,0,0,5);
            LinearLayout ll = (LinearLayout) findViewById(R.id.sim_route_list);
            ll.addView(entry);
            entry.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    simRouteListClick(v);
                }
            });

        }
    }
}
