package com.quickshare.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quickshare.app.data.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onCopy: (HistoryItem) -> Unit,
    private val onOpen: (HistoryItem) -> Unit,
    private val onDelete: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    private val items = mutableListOf<HistoryItem>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun submit(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.link.text = item.link
        holder.date.text = dateFormat.format(Date(item.createdAt))

        holder.copy.setOnClickListener { onCopy(item) }
        holder.delete.setOnClickListener { onDelete(item) }
        holder.itemView.setOnClickListener { onOpen(item) }
        holder.itemView.setOnLongClickListener {
            onCopy(item)
            true
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.itemName)
        val link: TextView = view.findViewById(R.id.itemLink)
        val date: TextView = view.findViewById(R.id.itemDate)
        val copy: ImageButton = view.findViewById(R.id.itemCopy)
        val delete: ImageButton = view.findViewById(R.id.itemDelete)
    }
}
