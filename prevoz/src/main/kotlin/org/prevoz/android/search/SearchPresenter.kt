package org.prevoz.android.search

class SearchPresenter {

    var view: SearchView? = null

    fun bind(view: SearchView) {
        this.view = view
    }

    fun unbind() {
        this.view = null
    }
}

interface SearchView {


}