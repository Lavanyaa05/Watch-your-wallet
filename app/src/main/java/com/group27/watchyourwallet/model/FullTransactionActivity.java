package com.group27.watchyourwallet.model;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.group27.watchyourwallet.R;
import com.group27.watchyourwallet.repository.ReceiptRepository;

import java.util.ArrayList;
import java.util.List;

public class FullTransactionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_transaction);

        String category = getIntent().getStringExtra("category");

        // reuse same repository
        ReceiptRepository repository = new ReceiptRepository();

        repository.getReceipts("user_1", receipts -> {

            List<Transaction> list = new ArrayList<>();

            for (Receipt r : receipts) {

                if (category.equals("All") || r.getCategory().equals(category)) {
                    list.add(new Transaction(
                            r.getStoreName(),
                            r.getAmount(),
                            r.getCategory(),
                            r.getDate()
                    ));
                }
            }

            runOnUiThread(() -> {

                LinearLayout container = findViewById(R.id.transactionContainer);

                container.removeAllViews(); // IMPORTANT

                for (Transaction t : list) {
                    View view = getLayoutInflater().inflate(R.layout.item_transaction, container, false);

                    ((TextView) view.findViewById(R.id.tv_name))
                            .setText(t.storeName);

                    ((TextView) view.findViewById(R.id.tv_amount))
                            .setText("$" + String.format("%.2f", t.amount));

                    container.addView(view);
                }
            });
        });
    }
}
