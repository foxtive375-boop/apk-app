package com.foxtive.dominofever.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.foxtive.dominofever.R
import com.foxtive.dominofever.databinding.ItemDominoBinding
import com.foxtive.dominofever.model.Domino

class DominoHandAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<DominoHandAdapter.DominoViewHolder>() {

    private var hand: List<Domino> = emptyList()
    private var playableIndexes: Set<Int> = emptySet()
    private var selectedIndex: Int? = null

    fun submit(hand: List<Domino>, playableIndexes: Set<Int>, selectedIndex: Int?) {
        this.hand = hand
        this.playableIndexes = playableIndexes
        this.selectedIndex = selectedIndex
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DominoViewHolder {
        val binding = ItemDominoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DominoViewHolder(binding)
    }

    override fun getItemCount(): Int = hand.size

    override fun onBindViewHolder(holder: DominoViewHolder, position: Int) {
        holder.bind(
            domino = hand[position],
            isPlayable = position in playableIndexes,
            isSelected = selectedIndex == position,
            onClick = { onClick(position) }
        )
    }

    class DominoViewHolder(
        private val binding: ItemDominoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(domino: Domino, isPlayable: Boolean, isSelected: Boolean, onClick: () -> Unit) {
            binding.tvDomino.text = "${domino.left}\n${domino.right}"

            val context = binding.root.context
            val fillColor = when {
                isSelected -> ContextCompat.getColor(context, R.color.hand_selected)
                isPlayable -> ContextCompat.getColor(context, R.color.hand_playable)
                else -> ContextCompat.getColor(context, R.color.domino_face)
            }

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(fillColor)
                setStroke(
                    3,
                    ContextCompat.getColor(context, R.color.domino_stroke)
                )
            }
            binding.tvDomino.background = drawable
            binding.tvDomino.alpha = if (isPlayable || isSelected) 1f else 0.7f

            binding.root.setOnClickListener { onClick() }
        }
    }
}
