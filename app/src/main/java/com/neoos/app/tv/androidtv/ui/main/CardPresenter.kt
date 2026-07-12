package com.neoos.neotv.ui.main

import android.graphics.Color
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.neoos.neotv.R
import com.squareup.picasso.Picasso

/** Anything shown as a card in the browse grid implements this. */
interface CardItem {
    val cardTitle: String
    val cardSubtitle: String?
    val cardImageUrl: String?
    /** Local drawable fallback, used when there's no remote logo (e.g. action tiles). */
    val cardIconRes: Int?
}

private const val CARD_WIDTH = 313
private const val CARD_HEIGHT = 176

class CardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                super.setSelected(selected)
            }
        }
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.card_bg))
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardItem = item as CardItem
        val cardView = viewHolder.view as ImageCardView
        cardView.titleText = cardItem.cardTitle
        cardView.contentText = cardItem.cardSubtitle ?: ""
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.mainImageView.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE

        if (!cardItem.cardImageUrl.isNullOrBlank()) {
            Picasso.get()
                .load(cardItem.cardImageUrl)
                .resize(CARD_WIDTH, CARD_HEIGHT)
                .centerInside()
                .into(cardView.mainImageView)
            cardView.mainImageView.setPadding(0, 0, 0, 0)
        } else if (cardItem is AppCardItem) {
            // Try to load real app icon
            try {
                val pkg = cardItem.app.packageName
                val icon = cardView.context.packageManager.getApplicationIcon(pkg)
                cardView.mainImageView.setImageDrawable(icon)
                cardView.mainImageView.setPadding(30, 30, 30, 30)
                cardView.mainImageView.setBackgroundColor(Color.TRANSPARENT)
            } catch (_: Exception) {
                cardView.mainImageView.setImageResource(R.drawable.ic_placeholder)
                cardView.mainImageView.setPadding(40, 40, 40, 40)
                cardView.mainImageView.setBackgroundColor(Color.parseColor("#1E1E1E"))
            }
        } else {
            cardView.mainImageView.setImageResource(cardItem.cardIconRes ?: R.drawable.ic_placeholder)
            cardView.mainImageView.setPadding(40, 40, 40, 40)
            cardView.mainImageView.setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null
    }
}
