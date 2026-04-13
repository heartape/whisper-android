package com.heartape.whisper.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreenTemplate(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp)
        ) {
            item { content() }
        }
    }
}


@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    DocumentScreenTemplate(title = "用户协议", onBack = onBack) {
        Column {
            Text("最后更新时间：2026年4月8日\n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

            SectionTitle("1. 协议的接受与修改")
            SectionBody("欢迎使用耳语（Whisper）！本协议是您与 Whisper Inc. 之间关于注册、登录、使用本应用服务所订立的协议。您在使用本服务前，必须仔细阅读本协议。当您注册成功，即表示您已充分阅读、理解并接受本协议的全部内容。")

            SectionTitle("2. 服务内容与规范")
            SectionBody("耳语提供即时通讯、群组聊天、文件传输等功能。您在使用本服务时必须遵守法律法规，不得利用本应用从事任何违法违规、侵犯他人合法权益的行为。包括但不限于发布涉黄、涉暴、涉政敏感信息。违者我们将保留封停账号的权利。")

            SectionTitle("3. 账号安全")
            SectionBody("您的账号由您自行保管。您须对使用该账号下所发生的一切行为负责。我们建议您设置高强度密码并妥善保管您的短信验证码。若发现账号遭到未授权使用，请立即联系我们。")

            SectionTitle("4. 免责声明")
            SectionBody("对于因不可抗力（如网络故障、黑客攻击、政府行为等）导致的系统中断或数据丢失，耳语平台将尽力协助恢复，但不对上述不可抗力造成的直接或间接损失承担责任。")
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    DocumentScreenTemplate(title = "隐私政策", onBack = onBack) {
        Column {
            Text("最后更新时间：2026年4月8日\n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

            SectionTitle("1. 我们如何收集您的信息")
            SectionBody("为了向您提供基础的即时通讯服务，我们会收集您在注册时主动提供的手机号码、用户名、头像信息。在应用运行期间，我们可能会收集您的设备型号、操作系统版本等基础设备信息，用于优化崩溃和兼容性问题。")

            SectionTitle("2. 我们如何使用您的信息")
            SectionBody("我们收集的信息仅用于为您提供稳定的通讯服务。包括但不限于：同步您的历史聊天记录、生成群聊数据、识别异常账号环境。我们绝不会将您的个人信息出售给任何第三方广告公司。")

            SectionTitle("3. 数据存储与安全")
            SectionBody("您的通信内容在传输过程中经过高强度加密保护。我们的数据库对您的密码及核心敏感字段进行了单向 Hash 加密存储，即便是平台内部员工也无法获取您的真实明文信息。您产生的本地聊天记录缓存将安全存储于您设备的独立沙盒目录中。")

            SectionTitle("4. 您的权利")
            SectionBody("您拥有管理自己信息的完整权利。您可以在“设置-个人资料”中随时修改您的头像、昵称和签名。您也有权在“设置-账号安全”中选择注销账号。账号注销后，您在该平台的所有相关个人数据将被不可逆地永久抹除。")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
    )
}