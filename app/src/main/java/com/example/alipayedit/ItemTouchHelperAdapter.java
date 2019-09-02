package com.example.alipayedit;


import androidx.recyclerview.widget.RecyclerView;

public interface ItemTouchHelperAdapter {

    void onItemMove(RecyclerView.ViewHolder holder, int fromPosition, int targetPosition);

    void onItemSelect(RecyclerView.ViewHolder holder);

    void onItemClear(RecyclerView.ViewHolder holder);

    void onItemDismiss(RecyclerView.ViewHolder holder);
}
