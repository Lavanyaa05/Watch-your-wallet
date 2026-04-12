package com.group27.watchyourwallet.model;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.group27.watchyourwallet.R;
import com.group27.watchyourwallet.repository.DataRefreshManager;
import com.group27.watchyourwallet.repository.ReceiptRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvTotal;
    private ReceiptRepository repository;

    private DataRefreshManager.RefreshListener refreshListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dashboard, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        tvTotal = view.findViewById(R.id.tvTotal);

        repository = new ReceiptRepository();

        setupPieChart();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔥 SINGLE CLEAN LISTENER (NO RECREATION BUG)
        refreshListener = new DataRefreshManager.RefreshListener() {
            @Override
            public void onDataChanged() {
                loadData();
            }
        };

        DataRefreshManager.setListener(refreshListener);

        loadData();
    }

    private void setupPieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);

        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
    }

    private void loadData() {

        Log.d("PIE_DEBUG", "loadData() CALLED");
        Toast.makeText(getContext(), "Dashboard refresh", Toast.LENGTH_SHORT).show();

        repository.getCategoryTotals(new ReceiptRepository.CategoryCallback() {

            @Override
            public void onSuccess(Map<String, Double> data) {

                if (!isAdded()) return; // 🔥 FIX FRAGMENT SAFETY

                requireActivity().runOnUiThread(() -> {

                    if (data == null || data.isEmpty()) {
                        pieChart.clear();
                        tvTotal.setText("No data yet");
                        return;
                    }

                    double total = 0;
                    List<PieEntry> entries = new ArrayList<>();

                    for (String category : data.keySet()) {
                        double amount = data.get(category);
                        total += amount;
                        entries.add(new PieEntry((float) amount, category));
                    }

                    tvTotal.setText("Total: $" + String.format("%.2f", total));

                    pieChart.clear(); // remove old dataset first

                    PieDataSet dataSet = new PieDataSet(entries, "Spending");
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    dataSet.setSliceSpace(3f);
                    dataSet.setSelectionShift(5f);

                    PieData pieData = new PieData(dataSet);
                    pieData.setValueTextSize(12f);
                    pieData.setValueTextColor(Color.WHITE);

                    pieChart.setData(pieData);

                    // HARD REFRESH SEQUENCE
                    pieChart.notifyDataSetChanged();
                    pieChart.invalidate();
                    pieChart.animateY(800);
                });
                Log.d("PIE", "Updating chart with size: " + data.size());
            }

            @Override
            public void onFailure(Throwable t) {
                if (!isAdded()) return;

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(); //ALWAYS refresh when user opens Home tab
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // PREVENT MEMORY LEAK
        DataRefreshManager.setListener(null);
    }
}