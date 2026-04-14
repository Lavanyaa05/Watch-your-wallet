package com.group27.watchyourwallet;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.group27.watchyourwallet.model.FullTransactionActivity;
import com.group27.watchyourwallet.model.Receipt;
import com.group27.watchyourwallet.repository.DataRefreshManager;
import com.group27.watchyourwallet.repository.ReceiptRepository;
import com.group27.watchyourwallet.model.Transaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends BaseActivity {

    private PieChart pieChart;
    private ReceiptRepository repository;
    private static final String USER_ID = "user_1";

    private List<Transaction> allTransactions = new ArrayList<>();
    private String selectedCategory = "All";
    private boolean isFullView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
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

        DataRefreshManager.setListener(() -> loadRealData());
        loadRealData();

        Button seeAll = findViewById(R.id.btnSeeAll);
        seeAll.setOnClickListener(v -> {
            isFullView = true;
            refreshUI();
        });

        // News Card 1 — replace the URL with your actual link
        CardView newsCard1 = findViewById(R.id.newsCard1);
        newsCard1.setOnClickListener(v -> {
            String url = "https://cnalifestyle.channelnewsasia.com/advertorial/4-money-habits-adopt-your-first-salary-retirement-413161"; // REPLACE WITH YOUR URL
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        // News Card 2 — replace the URL with your actual link
        CardView newsCard2 = findViewById(R.id.newsCard2);
        newsCard2.setOnClickListener(v -> {
            String url = "https://www.dbs.com.sg/personal/deposits/bank-with-ease/protecting-yourself-online"; // REPLACE WITH YOUR URL
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
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

                    loadTransactions();
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("HomeActivity", "Failed: " + t.getMessage());
                runOnUiThread(() -> setupEmptyChart());
            }
        });
    }

    private void setupRealChart(Map<String, Double> categoryTotals) {
        double total = 0;
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        java.util.HashMap<String, Integer> categoryColors = new java.util.HashMap<>();
        categoryColors.put("Food & Dining",  Color.parseColor("#FF0000"));
        categoryColors.put("Transport",      Color.parseColor("#00B894"));
        categoryColors.put("Shopping",       Color.parseColor("#FD79A8"));
        categoryColors.put("Entertainment",  Color.parseColor("#6C5CE7"));
        categoryColors.put("Uncategorised",  Color.parseColor("#636E72"));

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            total += entry.getValue();
            Integer color = categoryColors.get(entry.getKey());
            colors.add(color != null ? color : Color.parseColor("#B2BEC3"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(4f);
        dataSet.setSelectionShift(8f);

        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));

        PieData pieData = new PieData(dataSet);

        android.text.SpannableString centerText = new android.text.SpannableString(
                "SPENT\n$" + String.format("%.2f", total));
        centerText.setSpan(new android.text.style.RelativeSizeSpan(0.55f),
                0, 5, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        centerText.setSpan(new android.text.style.ForegroundColorSpan(
                        Color.parseColor("#888888")),
                0, 5, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        centerText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                6, centerText.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        centerText.setSpan(new android.text.style.ForegroundColorSpan(
                        Color.parseColor("#1A1A1A")),
                6, centerText.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(15f);
        pieChart.setCenterTextRadiusPercent(85f);
        pieChart.setExtraOffsets(20f, 20f, 20f, 20f);

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
        legend.setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);

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

        pieChart.setData(new PieData(dataSet));
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

    private void loadTransactions() {
        repository.getReceipts(USER_ID, receipts -> {
            if (receipts == null) return;

            List<Transaction> transactions = new ArrayList<>();

            for (Receipt r : receipts) {
                transactions.add(new Transaction(
                        r.getStoreName(),
                        r.getAmount(),
                        r.getCategory(),
                        r.getDate()
                ));
            }
            transactions.sort((t1, t2) -> {
                java.time.LocalDate d1 = parseDateSafe(t1.date);
                java.time.LocalDate d2 = parseDateSafe(t2.date);

                if (d1 == null || d2 == null) return 0;

                return d2.compareTo(d1);
            });

            runOnUiThread(() -> {

                allTransactions.clear();
                allTransactions.addAll(transactions);

                setupTabs();

                selectedCategory = "Food & Dining";

                TabLayout tabLayout = findViewById(R.id.tabLayout);
                if (tabLayout.getTabCount() > 0) {
                    tabLayout.selectTab(tabLayout.getTabAt(0));
                }

                isFullView = false;

                refreshUI();
            });
        });
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        selectedCategory = "Food & Dining";
        tabLayout.selectTab(tabLayout.getTabAt(0));

        tabLayout.clearOnTabSelectedListeners();

        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Food & Dining"));
            tabLayout.addTab(tabLayout.newTab().setText("Transport"));
            tabLayout.addTab(tabLayout.newTab().setText("Entertainment"));
            tabLayout.addTab(tabLayout.newTab().setText("Shopping"));
            tabLayout.addTab(tabLayout.newTab().setText("Uncategorised"));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedCategory = tab.getText().toString();
                isFullView = false;
                refreshUI();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void refreshUI() {

        List<Transaction> filtered = getFilteredList();

        List<Transaction> toShow;

        if (isFullView) {
            toShow = filtered;
        } else {
            toShow = filtered.size() > 5 ? filtered.subList(0, 5) : filtered;
        }

        displayTransactions(toShow);
    }

    private List<Transaction> getFilteredList() {

        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : allTransactions) {

            if (t.category.equals(selectedCategory)) {
                filtered.add(t);
            }
        }

        return filtered;
    }

    private void displayTransactions(List<Transaction> transactions) {
        LinearLayout container = findViewById(R.id.transactionContainer);
        container.removeAllViews();

        for (Transaction t : transactions) {
            View view = getLayoutInflater().inflate(R.layout.item_transaction, container, false);

            TextView name = view.findViewById(R.id.tv_name);
            TextView amount = view.findViewById(R.id.tv_amount);

            name.setText(t.storeName);
            amount.setText("$" + String.format("%.2f", t.amount));

            container.addView(view, 0);
        }
    }

    public List<Transaction> getAllTransactions() {
        return allTransactions;
    }

    private java.time.LocalDate parseDateSafe(String dateStr) {

        try {
            java.time.format.DateTimeFormatter[] formats = new java.time.format.DateTimeFormatter[] {

                    java.time.format.DateTimeFormatter.ofPattern("d/M/yy"),
                    java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
                    java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy"),
                    java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"),
                    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"),
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            };

            for (java.time.format.DateTimeFormatter f : formats) {
                try {
                    return java.time.LocalDate.parse(dateStr, f);
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataRefreshManager.setListener(null);
    }
}