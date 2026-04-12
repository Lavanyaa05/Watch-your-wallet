package com.group27.watchyourwallet.model;

import static android.app.PendingIntent.getActivity;

import android.os.Bundle;
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
import com.github.mikephil.charting.utils.ColorTemplate;
import com.group27.watchyourwallet.R;

import com.group27.watchyourwallet.repository.ReceiptRepository;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DashboardFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvTotal;
    private ReceiptRepository repository;

    private static final String USER_ID = "user_1";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dashboard, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        tvTotal = view.findViewById(R.id.tvTotal);

        repository = new ReceiptRepository("");

        loadData();

        return view;
    }

    private void loadData() {

        repository.getCategoryTotals(USER_ID,
                new ReceiptRepository.OnCategoryTotalsListener() {

                    @Override
                    public void onSuccess(HashMap<String, Double> data) {

                        if (getActivity() == null) return;

                        getActivity().runOnUiThread(() -> {

                            double total = 0;
                            List<PieEntry> entries = new ArrayList<>();

                            for (String category : data.keySet()) {
                                double amount = data.get(category);
                                total += amount;
                                entries.add(new PieEntry((float) amount, category));
                            }

                            tvTotal.setText("Total: $" + total);

                            PieDataSet dataSet = new PieDataSet(entries, "Spending");
                            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                            PieData pieData = new PieData(dataSet);
                            pieChart.setData(pieData);

                            pieChart.invalidate(); // refresh
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
    }
}
