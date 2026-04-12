package com.group27.watchyourwallet;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.group27.watchyourwallet.repository.DataRefreshManager;
import com.group27.watchyourwallet.repository.ReceiptRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HomeActivity extends BaseActivity {

    private PieChart pieChart;
    private ReceiptRepository repository;
    private static final String USER_ID = "user1";

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

        pieChart = findViewById(R.id.pieChart);
        repository = new ReceiptRepository();

        // Listen for new receipts being saved
        DataRefreshManager.setListener(() -> loadRealData());

        loadRealData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register listener and reload every time user comes back to this screen
        DataRefreshManager.setListener(() -> loadRealData());
        loadRealData();
    }

    private void loadRealData() {
        repository.getCategoryTotals(new ReceiptRepository.CategoryCallback() {
            @Override
            public void onSuccess(Map<String, Double> data) {
                runOnUiThread(() -> {
                    if (data == null || data.isEmpty()) {
                        setupEmptyChart();
                        return;
                    }
                    setupRealChart(data);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("HomeActivity", "Failed to load data: " + t.getMessage());
                runOnUiThread(() -> setupEmptyChart());
            }
        });
    }

    private void setupRealChart(Map<String, Double> categoryTotals) {
        double total = 0;
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        // Fixed color per category — never changes regardless of order
        java.util.HashMap<String, Integer> categoryColors = new java.util.HashMap<>();
        categoryColors.put("Food & Dining",     Color.parseColor("#FF0000")); // red
        categoryColors.put("Transport",         Color.parseColor("#00B894")); // green
        categoryColors.put("Shopping",          Color.parseColor("#FD79A8")); // pink
        categoryColors.put("Entertainment",     Color.parseColor("#0984E3")); // blue
        categoryColors.put("Uncategorised",     Color.parseColor("#636E72")); // grey

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            total += entry.getValue();

            // Get the fixed color for this category, default to grey if unknown
            Integer color = categoryColors.get(entry.getKey());
            colors.add(color != null ? color : Color.parseColor("#B2BEC3"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText("Total\n$" + String.format("%.2f", total));
        pieChart.setCenterTextSize(13f);
        pieChart.setCenterTextColor(Color.parseColor("#1A1A1A"));

        com.github.mikephil.charting.components.Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(
                com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(
                com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(
                com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#1A1A1A"));
        legend.setWordWrapEnabled(true);
        legend.setXEntrySpace(12f);
        legend.setYEntrySpace(5f);

        pieChart.setTouchEnabled(true);
        pieChart.notifyDataSetChanged();
        pieChart.invalidate();
        pieChart.animateY(1000);
    }

    private void setupEmptyChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(1f, "No data yet"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Arrays.asList(Color.parseColor("#EEEEEE")));
        dataSet.setDrawValues(false);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(55f);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText("No expenses\nyet");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#999999"));
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.invalidate();
    }
}