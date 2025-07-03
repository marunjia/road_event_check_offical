package com.yuce.listener;

import com.yuce.entity.ImageResult;

import java.util.List;

// 你自己定义的接口，不需要引入任何依赖
public interface DataCompleteListener {
    void onAllDataReceived(List<ImageResult> allData);
}
