package com.group27.watchyourwallet;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.Arrays;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_scan) {
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
        setupPieChart();
    }

    private void setupPieChart() {
        PieChart pieChart = findViewById(R.id.pieChart);

        float spent = 2450f;
        float budget = 4000f;
        float remaining = budget - spent;

        PieDataSet dataSet = new PieDataSet(Arrays.asList(
                new PieEntry(spent, "Spent"),
                new PieEntry(remaining, "Remaining")
        ), "");

        dataSet.setColors(Arrays.asList(
                Color.parseColor("#9C8FD9"),
                Color.parseColor("#EEEEEE")
        ));
        dataSet.setSliceSpace(2f);
        dataSet.setDrawValues(false);

        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(72f);
        pieChart.setTransparentCircleRadius(75f);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText("Left to spend\n$" + (int) remaining);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#1A1A1A"));
        pieChart.setTouchEnabled(false);
        pieChart.invalidate();
    }
}