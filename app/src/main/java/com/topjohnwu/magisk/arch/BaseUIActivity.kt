package com.topjohnwu.magisk.arch

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.use
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.topjohnwu.magisk.BR
import com.topjohnwu.magisk.arch.inflater.LayoutInflaterFactory
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.base.BaseActivity

abstract class BaseUIActivity<VM : BaseViewModel, Binding : ViewDataBinding> :
    BaseActivity(), BaseUIComponent<VM> {

    protected lateinit var binding: Binding
    protected abstract val layoutRes: Int

    private val navHostFragment by lazy {
        supportFragmentManager.findFragmentById(navHostId) as? NavHostFragment
    }
    private val topFragment get() = navHostFragment?.childFragmentManager?.fragments?.getOrNull(0)
    protected val currentFragment get() = topFragment as? BaseUIFragment<*, *>

    override val viewRoot: View get() = binding.root
    open val navigation: NavController? get() = navHostFragment?.navController

    open val navHostId: Int = 0
    open val snackbarView get() = binding.root

    init {
        val theme = Config.darkTheme
        AppCompatDelegate.setDefaultNightMode(theme)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LayoutInflaterFactory(delegate)

        super.onCreate(savedInstanceState)

        startObserveEvents()

        // We need to set the window background explicitly since for whatever reason it's not
        // propagated upstream
        obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
            .use { it.getDrawable(0) }
            .also { window.setBackgroundDrawable(it) }

        window.decorView.windowInsetsController?.hide(WindowInsets.Type.statusBars())
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.navigationBars())
        window.decorView.windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.setDecorFitsSystemWindows(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window?.decorView?.post {
                // If navigation bar is short enough (gesture navigation enabled), make it transparent
                if (window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.navigationBars()) == false) {
                    window.navigationBarColor = Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        window.navigationBarDividerColor = Color.TRANSPARENT
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                        window.isStatusBarContrastEnforced = false
                    }
                }
            }
        }
    }

    fun setContentView() {
        binding = DataBindingUtil.setContentView<Binding>(this, layoutRes).also {
            it.setVariable(BR.viewModel, viewModel)
            it.lifecycleOwner = this
        }
    }

    fun setAccessibilityDelegate(delegate: View.AccessibilityDelegate?) {
        viewRoot.rootView.accessibilityDelegate = delegate
    }

    override fun onResume() {
        super.onResume()
        viewModel.requestRefresh()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return currentFragment?.onKeyEvent(event) == true || super.dispatchKeyEvent(event)
    }

    override fun onEventDispatched(event: ViewEvent) = when (event) {
        is ContextExecutor -> event(this)
        is ActivityExecutor -> event(this)
        else -> Unit
    }

    override fun onBackPressed() {
        if (navigation == null || currentFragment?.onBackPressed()?.not() == true) {
            super.onBackPressed()
        }
    }

    fun NavDirections.navigate() {
        navigation?.navigate(this)
    }
}
