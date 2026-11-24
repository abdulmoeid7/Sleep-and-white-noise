package com.example.sleepandwhitenoise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sleepandwhitenoise.R
import com.example.sleepandwhitenoise.SleepApp
import com.example.sleepandwhitenoise.data.AuthRepository
import com.example.sleepandwhitenoise.ui.Routes
import com.example.sleepandwhitenoise.ui.components.AppTextField
import com.example.sleepandwhitenoise.ui.components.InfoNote
import com.example.sleepandwhitenoise.ui.components.PrimaryButton
import com.example.sleepandwhitenoise.ui.components.ScreenScaffold
import com.example.sleepandwhitenoise.ui.components.StateContent
import com.example.sleepandwhitenoise.ui.components.TextAction
import com.example.sleepandwhitenoise.ui.components.VSpace
import com.example.sleepandwhitenoise.ui.resetTo
import com.example.sleepandwhitenoise.ui.theme.AppTheme
import com.example.sleepandwhitenoise.ui.theme.Space
import kotlinx.coroutines.launch

private const val PLACEHOLDER_NOTE =
    "Accounts are optional and not connected to a server yet. Everything works as a guest, and your data stays on this device."

@Composable
private fun FormError(text: String?) {
    if (text != null) {
        VSpace(Space.s)
        Text(text, color = AppTheme.colors.error, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Runs an auth call with a loading flag and routes errors to the form. */
@Composable
private fun rememberSubmitter(): Triple<Boolean, String?, (suspend () -> AuthRepository.Result, () -> Unit) -> Unit> {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val submit: (suspend () -> AuthRepository.Result, () -> Unit) -> Unit = { call, onOk ->
        if (!loading) scope.launch {
            loading = true; error = null
            when (val r = call()) {
                AuthRepository.Result.Ok -> onOk()
                is AuthRepository.Result.Error -> error = r.message
            }
            loading = false
        }
    }
    return Triple(loading, error, submit)
}

@Composable
fun LoginScreen(app: SleepApp, nav: NavHostController) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val (loading, error, submit) = rememberSubmitter()
    val signIn = {
        submit({ app.auth.signIn(email, password) }) {
            app.settings.update { it.copy(onboarded = true) }
            nav.resetTo(Routes.HOME)
        }
    }
    ScreenScaffold(
        title = "Welcome back", subtitle = "Sign in to your optional account.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Sign in", signIn, loading = loading, enabled = email.isNotBlank() && password.isNotBlank())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextAction("Create account", { nav.navigate(Routes.SIGNUP) })
                TextAction("Continue as guest", {
                    app.settings.update { it.copy(onboarded = true) }
                    if (!nav.popBackStack()) nav.resetTo(Routes.HOME)
                }, color = AppTheme.colors.muted)
            }
        },
    ) {
        Image(painterResource(R.drawable.app_logo), null, Modifier.size(72.dp))
        VSpace(Space.xl)
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            AppTextField(email, { email = it }, "Email address", leadingIcon = R.drawable.ic_mail, keyboardType = KeyboardType.Email, enabled = !loading)
            AppTextField(password, { password = it }, "Password", leadingIcon = R.drawable.ic_lock, password = true,
                imeAction = ImeAction.Done, onIme = signIn, enabled = !loading)
        }
        TextAction("Forgot password?", { nav.navigate(Routes.FORGOT) }, Modifier.align(Alignment.End))
        FormError(error)
        VSpace(Space.l)
        InfoNote(PLACEHOLDER_NOTE)
    }
}

@Composable
fun SignUpScreen(app: SleepApp, nav: NavHostController) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    val (loading, error, submit) = rememberSubmitter()
    val c = AppTheme.colors
    ScreenScaffold(
        title = "Create account", subtitle = "Create an optional account.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Create account", {
                submit({ app.auth.signUp(name, email, password) }) {
                    app.settings.update { it.copy(onboarded = true) }
                    nav.resetTo(Routes.HOME)
                }
            }, loading = loading, enabled = accepted && name.isNotBlank() && email.isNotBlank() && password.isNotBlank())
            TextAction("Already have an account? Sign in", { nav.popBackStack() }, Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            AppTextField(name, { name = it }, "Name", leadingIcon = R.drawable.ic_profile, enabled = !loading)
            AppTextField(email, { email = it }, "Email address", leadingIcon = R.drawable.ic_mail, keyboardType = KeyboardType.Email,
                error = if (email.isNotBlank()) app.auth.validateEmail(email) else null, enabled = !loading)
            AppTextField(password, { password = it }, "Password", leadingIcon = R.drawable.ic_lock, password = true,
                error = if (password.isNotEmpty()) app.auth.validatePassword(password) else null, imeAction = ImeAction.Done, enabled = !loading)
        }
        VSpace(Space.s)
        Row(
            Modifier.fillMaxWidth().toggleable(accepted, role = Role.Checkbox) { accepted = it },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(accepted, null, colors = CheckboxDefaults.colors(checkedColor = c.primary, checkmarkColor = c.onPrimary, uncheckedColor = c.muted))
            Text("I accept the terms and privacy policy", color = c.text, style = MaterialTheme.typography.bodyMedium)
        }
        FormError(error)
        VSpace(Space.l)
        InfoNote(PLACEHOLDER_NOTE)
    }
}

@Composable
fun ForgotPasswordScreen(app: SleepApp, nav: NavHostController) {
    var email by rememberSaveable { mutableStateOf("") }
    val (loading, error, submit) = rememberSubmitter()
    ScreenScaffold(
        title = "Forgot password", subtitle = "Enter your account email to request a reset.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Send code", { submit({ app.auth.requestReset(email) }) { nav.navigate(Routes.VERIFY) } },
                loading = loading, enabled = email.isNotBlank())
        },
    ) {
        AppTextField(email, { email = it }, "Email address", leadingIcon = R.drawable.ic_mail, keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done, enabled = !loading)
        FormError(error)
    }
}

@Composable
fun VerifyCodeScreen(app: SleepApp, nav: NavHostController) {
    var code by rememberSaveable { mutableStateOf("") }
    var resent by remember { mutableStateOf(false) }
    val (loading, error, submit) = rememberSubmitter()
    ScreenScaffold(
        title = "Reset verification", subtitle = "Enter the code sent to your email.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Verify code", { submit({ app.auth.verifyCode(code) }) { nav.navigate(Routes.NEW_PASSWORD) } },
                loading = loading, enabled = code.length == 6)
        },
    ) {
        AppTextField(code, { v -> code = v.filter { it.isDigit() }.take(6) }, "Verification code",
            keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done, enabled = !loading)
        TextAction(if (resent) "Code sent again" else "Resend code", { resent = true }, Modifier.align(Alignment.End))
        FormError(error)
    }
}

@Composable
fun NewPasswordScreen(app: SleepApp, nav: NavHostController) {
    var pw by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val (loading, error, submit) = rememberSubmitter()
    ScreenScaffold(
        title = "New password", subtitle = "Choose a new secure password.", onBack = { nav.popBackStack() },
        bottom = {
            PrimaryButton("Reset password", {
                submit({ app.auth.setNewPassword(pw, confirm) }) {
                    nav.navigate(Routes.RESET_DONE) { popUpTo(Routes.FORGOT) { inclusive = true } }
                }
            }, loading = loading, enabled = pw.isNotEmpty() && confirm.isNotEmpty())
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            AppTextField(pw, { pw = it }, "New password", leadingIcon = R.drawable.ic_lock, password = true,
                error = if (pw.isNotEmpty()) app.auth.validatePassword(pw) else null, enabled = !loading)
            AppTextField(confirm, { confirm = it }, "Confirm password", leadingIcon = R.drawable.ic_lock, password = true,
                imeAction = ImeAction.Done, error = if (confirm.isNotEmpty() && confirm != pw) "Passwords don't match" else null, enabled = !loading)
        }
        FormError(error)
    }
}

@Composable
fun ResetSuccessScreen(nav: NavHostController) {
    ScreenScaffold(
        title = null, onBack = { nav.popBackStack() },
        bottom = { PrimaryButton("Back to login", { nav.popBackStack(Routes.LOGIN, inclusive = false) || nav.popBackStack() }) },
    ) {
        StateContent(null, "Password reset", "Your password has been updated.", success = true)
    }
}
