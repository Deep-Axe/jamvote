package com.jamvote.app.queue;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.jamvote.app.model.Song;
import com.jamvote.app.util.FirebaseUtil;

import java.util.List;

public class QueueViewModel extends ViewModel {
    private final MutableLiveData<List<Song>> queueItems = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> historyItems = new MutableLiveData<>();
    private ListenerRegistration queueListener;
    private ListenerRegistration historyListener;

    public LiveData<List<Song>> getQueueItems() { return queueItems; }
    public LiveData<List<Song>> getHistoryItems() { return historyItems; }

    public void startListening(String roomId) {
        if (queueListener != null) queueListener.remove();
        if (historyListener != null) historyListener.remove();
        queueListener = FirebaseUtil.listenToQueue(roomId, queueItems::postValue);
        historyListener = FirebaseUtil.listenToHistory(roomId, historyItems::postValue);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (queueListener != null) queueListener.remove();
        if (historyListener != null) historyListener.remove();
    }
}
