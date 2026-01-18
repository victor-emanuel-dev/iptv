package com.ivip.iptvpremium2016

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChannelsAdapter(
    private val channels: List<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelsAdapter.ChannelViewHolder>() {

    private var selectedPosition = 0

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
        val tvChannelCategory: TextView = itemView.findViewById(R.id.tvChannelCategory)
        val tvChannelDescription: TextView = itemView.findViewById(R.id.tvChannelDescription)
        val viewLiveIndicator: View = itemView.findViewById(R.id.viewLiveIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]

        holder.tvChannelName.text = channel.name
        holder.tvChannelCategory.text = channel.category
        holder.tvChannelDescription.text = channel.description
        holder.viewLiveIndicator.visibility = if (channel.isLive) View.VISIBLE else View.GONE

        // Destacar canal selecionado com cores adequadas
        val isSelected = position == selectedPosition
        val context = holder.itemView.context

        if (isSelected) {
            // Canal selecionado - fundo laranja, textos brancos
            holder.itemView.setBackgroundColor(context.getColor(R.color.primary_color))
            holder.tvChannelName.setTextColor(context.getColor(R.color.white))
            holder.tvChannelDescription.setTextColor(context.getColor(R.color.white))
            holder.tvChannelCategory.setTextColor(context.getColor(R.color.white))
            holder.tvChannelCategory.setBackgroundColor(context.getColor(R.color.white))
            holder.tvChannelCategory.setTextColor(context.getColor(R.color.primary_color))
        } else {
            // Canal não selecionado - cores padrão
            holder.itemView.setBackgroundColor(context.getColor(R.color.surface_dark))
            holder.tvChannelName.setTextColor(context.getColor(R.color.white))
            holder.tvChannelDescription.setTextColor(context.getColor(R.color.text_secondary))
            holder.tvChannelCategory.setTextColor(context.getColor(R.color.primary_color))
            holder.tvChannelCategory.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                val oldPosition = selectedPosition
                selectedPosition = currentPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onChannelClick(channel)
            }
        }
    }

    override fun getItemCount() = channels.size
}