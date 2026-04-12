package com.group27.watchyourwallet.repository;

public class DataRefreshManager {

    public interface RefreshListener {
        void onDataChanged();
    }

    private static RefreshListener listener;

    public static void setListener(RefreshListener l) {
        listener = l;
    }

    public static void notifyDataChanged() {
        if (listener != null) {
            listener.onDataChanged();
        }
    }
}
