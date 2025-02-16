package io.github.wulkanowy.ui.modules.login.form

import androidx.core.net.toUri
import io.github.wulkanowy.data.db.entities.AdminMessage
import io.github.wulkanowy.data.enums.MessageType
import io.github.wulkanowy.data.flatResourceFlow
import io.github.wulkanowy.data.logResourceStatus
import io.github.wulkanowy.data.onResourceData
import io.github.wulkanowy.data.onResourceError
import io.github.wulkanowy.data.onResourceLoading
import io.github.wulkanowy.data.onResourceNotLoading
import io.github.wulkanowy.data.onResourceSuccess
import io.github.wulkanowy.data.repositories.PreferencesRepository
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.data.resourceFlow
import io.github.wulkanowy.domain.adminmessage.GetAppropriateAdminMessageUseCase
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.modules.login.LoginData
import io.github.wulkanowy.ui.modules.login.LoginErrorHandler
import io.github.wulkanowy.ui.modules.login.support.LoginSupportInfo
import io.github.wulkanowy.utils.AnalyticsHelper
import io.github.wulkanowy.utils.AppInfo
import io.github.wulkanowy.utils.ifNullOrBlank
import timber.log.Timber
import java.net.URL
import javax.inject.Inject

class LoginFormPresenter @Inject constructor(
    studentRepository: StudentRepository,
    private val loginErrorHandler: LoginErrorHandler,
    private val appInfo: AppInfo,
    private val analytics: AnalyticsHelper,
    private val getAppropriateAdminMessageUseCase: GetAppropriateAdminMessageUseCase,
    private val preferencesRepository: PreferencesRepository,
) : BasePresenter<LoginFormView>(loginErrorHandler, studentRepository) {

    private var lastError: Throwable? = null

    override fun onAttachView(view: LoginFormView) {
        super.onAttachView(view)
        view.run {
            initView()
            showContact(false)
            showOtherOptionsButton(appInfo.isDebug)
            showVersion()

            loginErrorHandler.onBadCredentials = {
                setErrorPassIncorrect(it.takeIf { !it.isNullOrBlank() })
                showSoftKeyboard()
                Timber.i("Entered wrong username or password")
            }
        }

        reloadAdminMessage()
    }

    private fun reloadAdminMessage() {
        flatResourceFlow {
            getAppropriateAdminMessageUseCase(
                scrapperBaseUrl = view?.formHostValue.orEmpty(),
                type = MessageType.LOGIN_MESSAGE,
            )
        }
            .logResourceStatus("load login admin message")
            .onResourceData { view?.showAdminMessage(it) }
            .onResourceError { view?.showAdminMessage(null) }
            .launch()
    }

    fun onAdminMessageSelected(url: String?) {
        url?.let { view?.openInternetBrowser(it) }
    }

    fun onAdminMessageDismissed(adminMessage: AdminMessage) {
        preferencesRepository.dismissedAdminMessageIds += adminMessage.id

        view?.showAdminMessage(null)
    }

    fun onPrivacyLinkClick() {
        view?.openPrivacyPolicyPage()
    }

    fun onAdvancedLoginClick() {
        view?.openAdvancedLogin()
    }

    fun onHostSelected() {
        view?.apply {
            clearPassError()
            clearUsernameError()
            clearHostError()
            if (formHostValue.contains("fakelog")) {
                setCredentials("jan@fakelog.cf", "jan123")
            } else if (formUsernameValue == "jan@fakelog.cf" && formPassValue == "jan123") {
                setCredentials("", "")
            }
            updateCustomDomainSuffixVisibility()
            updateUsernameLabel()
            reloadAdminMessage()
        }
    }

    fun updateCustomDomainSuffixVisibility() {
        view?.run {
            showDomainSuffixInput("customSuffix" in formHostValue)
        }
    }

    fun updateUsernameLabel() {
        view?.run {
            setUsernameLabel(if ("email" !in formHostValue) nicknameLabel else emailLabel)
        }
    }

    fun onPassTextChanged() {
        view?.clearPassError()
    }

    fun onUsernameTextChanged() {
        view?.clearUsernameError()

        val username = view?.formUsernameValue.orEmpty().trim()
        if ("@" in username && "@vulcan" !in username) {
            val hosts = view?.getHostsValues().orEmpty().associateBy { it.toUri().host }
            val usernameHost = username.substringAfter("@")

            hosts[usernameHost]?.let {
                view?.run {
                    setHost(it)
                    clearHostError()
                }
            }
        }
    }

    private fun getLoginData(): LoginData {
        val email = view?.formUsernameValue.orEmpty().trim()
        val password = view?.formPassValue.orEmpty().trim()
        val host = view?.formHostValue.orEmpty().trim()
        val domainSuffix = view?.formDomainSuffix.orEmpty().trim().takeIf {
            "customSuffix" in host
        }.orEmpty()
        val symbol = view?.formHostSymbol.orEmpty().trim()

        return LoginData(
            login = email,
            password = password,
            baseUrl = host,
            domainSuffix = domainSuffix,
            symbol = symbol
        )
    }

    fun onSignInClick() {
        val loginData = getLoginData()

        if (!validateCredentials(loginData.login, loginData.password, loginData.baseUrl)) return

        resourceFlow {
            studentRepository.getUserSubjectsFromScrapper(
                email = loginData.login,
                password = loginData.password,
                scrapperBaseUrl = loginData.baseUrl,
                domainSuffix = loginData.domainSuffix,
                symbol = loginData.symbol.orEmpty(),
            )
        }
            .logResourceStatus("login")
            .onResourceLoading {
                view?.run {
                    hideSoftKeyboard()
                    showProgress(true)
                    showContent(false)
                }
            }
            .onResourceSuccess {
                when (it.symbols.size) {
                    0 -> view?.navigateToSymbol(loginData)
                    else -> view?.navigateToStudentSelect(loginData, it)
                }
                analytics.logEvent(
                    "registration_form",
                    "success" to true,
                    "scrapperBaseUrl" to loginData.baseUrl,
                    "error" to "No error"
                )
            }
            .onResourceNotLoading {
                view?.apply {
                    showProgress(false)
                    showContent(true)
                }
            }
            .onResourceError {
                loginErrorHandler.dispatch(it)
                lastError = it
                view?.showContact(true)
                analytics.logEvent(
                    "registration_form",
                    "success" to false,
                    "scrapperBaseUrl" to loginData.baseUrl,
                    "error" to it.message.ifNullOrBlank { "No message" }
                )
            }
            .launch("login")
    }

    fun onFaqClick() {
        view?.openFaqPage()
    }

    fun onEmailClick() {
        view?.openEmail(
            LoginSupportInfo(
                loginData = getLoginData(),
                lastErrorMessage = lastError?.message,
                registerUser = null,
                enteredSymbol = null,
            )
        )
    }

    fun onRecoverClick() {
        view?.onRecoverClick()
    }

    private fun validateCredentials(login: String, password: String, host: String): Boolean {
        var isCorrect = true

        if (login.isEmpty()) {
            view?.setErrorUsernameRequired()
            isCorrect = false
        } else {
            if ("@" in login && "login" in host) {
                view?.setErrorLoginRequired()
                isCorrect = false
            }
            if ("@" !in login && "email" in host) {
                view?.setErrorEmailRequired()
                isCorrect = false
            }
            if ("@" in login && "||" !in login && "login" !in host && "email" !in host) {
                val emailHost = login.substringAfter("@")
                val emailDomain = URL(host).host
                if (!emailHost.equals(emailDomain, true)) {
                    view?.setErrorEmailInvalid(domain = emailDomain)
                    isCorrect = false
                }
            }
        }

        if (password.isEmpty()) {
            view?.setErrorPassRequired(focus = isCorrect)
            isCorrect = false
        }

        if (password.length < 6 && password.isNotEmpty()) {
            view?.setErrorPassInvalid(focus = isCorrect)
            isCorrect = false
        }

        return isCorrect
    }
}
