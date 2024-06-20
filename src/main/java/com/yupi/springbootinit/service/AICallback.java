package com.yupi.springbootinit.service;

import com.yupi.springbootinit.model.entity.DialogueResponse;

public interface AICallback {
    void onDataReceived(DialogueResponse response);
    void onComplete();
    void onError(Exception e);
}
