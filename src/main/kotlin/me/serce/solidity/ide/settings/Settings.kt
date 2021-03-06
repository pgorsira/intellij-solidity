package me.serce.solidity.ide.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.util.io.isDirectory
import com.intellij.util.io.isFile
import com.intellij.util.xmlb.XmlSerializerUtil
import me.serce.solidity.ide.interop.Sol2JavaGenerationStyle
import org.jetbrains.annotations.Nls
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.swing.JComponent

@State(name = "SoliditySettings", storages = arrayOf(Storage("other.xml")))
class SoliditySettings : PersistentStateComponent<SoliditySettings> {
  var pathToEvm: String = ""
  var pathToDb: String = ""
  var solcPath: String = ""
  var useSolcEthereum: Boolean = true
  var useSolcJ: Boolean = false
  var generateJavaStubs: Boolean = false
  var dependenciesAutoRefresh: Boolean = false
  var basePackage: String = "com.myfirm.mypackage"
  var genStyle: Sol2JavaGenerationStyle = Sol2JavaGenerationStyle.WEB3J
  var genOutputPath: String = "src-gen"

  override fun getState(): SoliditySettings? {
    return this
  }

  override fun loadState(`object`: SoliditySettings) {
    XmlSerializerUtil.copyBean(`object`, this)
  }

  fun validateEvm(): Boolean {
    return validateEvm(pathToEvm)
  }

  companion object {

    fun getUrls(path: String): List<Path> {
      if (path.isBlank()) {
        return emptyList()
      }
      val p = Paths.get(path)

      val files : List<Path> = when {
        p.isDirectory() -> Files.list(p).collect(Collectors.toList())
        p.isFile() && p.toString().endsWith(".jar") -> listOf(p)
        else -> return emptyList()
      }
      return files
    }

    fun validateEvm(path: String): Boolean {
      return checkJars(path)
    }

    private fun checkJars(path: String): Boolean {
      val files = getUrls(path).toMutableList()
      
      if (files.isEmpty()) return false

      val ethJar = files.indexOfFirst { it.fileName.toString().contains("ethereumj") }
      if (ethJar > 0) {
        files[ethJar] = files[0].also { files[0] = files[ethJar] }
      }
      val cl = URLClassLoader(files.map { it.toUri().toURL() }.toTypedArray())
      try {
        Class.forName("org.ethereum.util.blockchain.StandaloneBlockchain", false, cl)
        return true
      } catch (e: ClassNotFoundException) {
        return false
      }
    }

    val instance: SoliditySettings
      get() = ServiceManager.getService(SoliditySettings::class.java)
  }
}

class SoliditySettingsConfigurable(private val mySettings: SoliditySettings) : SearchableConfigurable, Configurable.NoScroll {
  private var myPanel: SolidityConfigurablePanel? = null

  @Nls
  override fun getDisplayName(): String {
    return "Solidity"
  }

  override fun getHelpTopic(): String {
    return "preferences.Solidity"
  }

  override fun createComponent(): JComponent? {
    myPanel = SolidityConfigurablePanel()
    return myPanel!!.mainPanel
  }

  override fun isModified(): Boolean {
    return myPanel!!.isModified(mySettings)
  }

  @Throws(ConfigurationException::class)
  override fun apply() {
    myPanel!!.apply(mySettings)
  }

  override fun reset() {
    myPanel!!.reset(mySettings)
  }

  override fun disposeUIResources() {
    myPanel = null
  }

  override fun getId(): String {
    return helpTopic
  }

  fun getQuickFix(project: Project): Runnable {
    return Runnable { ShowSettingsUtil.getInstance().editConfigurable(project, this) }
  }
}

