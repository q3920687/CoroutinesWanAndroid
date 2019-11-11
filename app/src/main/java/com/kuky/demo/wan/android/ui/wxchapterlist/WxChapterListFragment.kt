package com.kuky.demo.wan.android.ui.wxchapterlist


import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.kuky.demo.wan.android.R
import com.kuky.demo.wan.android.base.*
import com.kuky.demo.wan.android.databinding.FragmentWxChapterListBinding
import com.kuky.demo.wan.android.ui.collection.CollectionModelFactory
import com.kuky.demo.wan.android.ui.collection.CollectionRepository
import com.kuky.demo.wan.android.ui.collection.CollectionViewModel
import com.kuky.demo.wan.android.ui.websitedetail.WebsiteDetailFragment
import com.kuky.demo.wan.android.ui.widget.ErrorReload
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

/**
 * @author Taonce.
 * @description 公众号作者对应的文章列表页
 */
class WxChapterListFragment : BaseFragment<FragmentWxChapterListBinding>() {
    companion object {
        /**
         * 公众号跳转到列表
         * [articleId] 文章id
         * [name] 作者名称
         */
        fun navigate(
            controller: NavController, @IdRes id: Int,
            articleId: Int, name: String
        ) = controller.navigate(id,
            Bundle().apply {
                putInt("articleId", articleId)
                putString("name", name)
            })
    }

    private val mAdapter by lazy { WxChapterListAdapter() }
    private val mViewMode by lazy {
        ViewModelProvider(requireActivity(), WxChapterListModelFactory(WxChapterListRepository()))
            .get(WxChapterListViewModel::class.java)
    }
    private val mCollectionViewModel by lazy {
        ViewModelProvider(requireActivity(), CollectionModelFactory(CollectionRepository()))
            .get(CollectionViewModel::class.java)
    }

    override fun getLayoutId(): Int = R.layout.fragment_wx_chapter_list

    override fun initFragment(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getInt("articleId")

        mBinding.wxChapter = arguments?.getString("name") ?: ""

        mBinding.refreshColor = R.color.colorAccent
        mBinding.refreshListener = SwipeRefreshLayout.OnRefreshListener {
            fetchWxChapterList(id)
        }

        mBinding.adapter = mAdapter
        mBinding.listener = OnItemClickListener { position, _ ->
            mAdapter.getItemData(position)?.let {
                WebsiteDetailFragment.viewDetail(
                    mNavController,
                    R.id.action_wxChapterListFragment_to_websiteDetailFragment,
                    it.link
                )
            }
        }
        mBinding.longClickListener = OnItemLongClickListener { position, _ ->
            mAdapter.getItemData(position)?.let { article ->
                requireContext().alert(
                    if (article.collect) "「${article.title}」已收藏"
                    else " 是否收藏 「${article.title}」"
                ) {
                    yesButton {
                        if (!article.collect) mCollectionViewModel.collectArticle(article.id, {
                            mViewMode.chapters?.value?.get(position)?.collect = true
                            requireContext().toast("收藏成功")
                        }, { message ->
                            requireContext().toast(message)
                        })
                    }
                    if (!article.collect) noButton { }
                }.show()
            }
            true
        }

        mBinding.errorReload = ErrorReload {
            fetchWxChapterList(id)
        }

        mBinding.gesture = DoubleClickListener(null, {
            mBinding.chapterList.scrollToTop()
        })

        fetchWxChapterList(id)
    }

    private fun fetchWxChapterList(id: Int?) {
        mViewMode.fetchWxArticles(id ?: 0) { code, _ ->
            when (code) {
                PAGING_THROWABLE_LOAD_CODE_INITIAL -> mBinding.errorStatus = true

                PAGING_THROWABLE_LOAD_CODE_AFTER -> requireContext().toast("加载更多出错啦~请检查网络")
            }
        }

        mBinding.refreshing = true
        mBinding.errorStatus = false

        mViewMode.chapters?.observe(this, Observer {
            mAdapter.submitList(it)
            delayLaunch(1000) {
                mBinding.refreshing = false
            }
        })
    }
}
