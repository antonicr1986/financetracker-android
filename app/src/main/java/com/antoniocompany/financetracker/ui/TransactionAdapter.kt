package com.antoniocompany.financetracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.antoniocompany.financetracker.R
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.databinding.ItemTransactionBinding
import com.google.android.material.color.MaterialColors

/**
 * Listado de movimientos.
 *
 * Hereda de ListAdapter, que compara la lista vieja con la nueva usando el
 * DiffUtil de abajo y anima solo lo que cambia. La alternativa,
 * notifyDataSetChanged(), redibuja todo y pierde la posicion del scroll.
 */
class TransactionAdapter(
    /** Al pulsar una fila: el panel abre ese movimiento para editarlo. */
    private val onClick: (TransactionDto) -> Unit
) : ListAdapter<TransactionDto, TransactionAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
        holder.itemView.setOnClickListener { onClick(transaction) }
    }

    class ViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionDto) {
            val context = binding.root.context
            val isIncome = transaction.type == TransactionType.INCOME

            binding.description.text = transaction.description
            binding.category.text = transaction.categoryName
                ?: context.getString(R.string.dashboard_no_category)
            binding.date.text = formatShortDate(transaction.date)

            // Menos (U+2212), no un guion: se alinea con las cifras.
            val sign = if (isIncome) "+" else "−"
            binding.amount.text = sign + formatCurrency(transaction.amount)

            binding.amount.setTextColor(
                if (isIncome) {
                    ContextCompat.getColor(context, R.color.income)
                } else {
                    MaterialColors.getColor(
                        binding.amount,
                        com.google.android.material.R.attr.colorOnSurface
                    )
                }
            )
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TransactionDto>() {
            override fun areItemsTheSame(old: TransactionDto, new: TransactionDto) =
                old.id == new.id

            // Las data class traen equals hecho, asi que esto compara campo a
            // campo sin escribirlo.
            override fun areContentsTheSame(old: TransactionDto, new: TransactionDto) =
                old == new
        }
    }
}
