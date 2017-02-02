package org.metaborg.spg.core

import com.google.inject.Singleton
import net.codingwell.scalaguice.ScalaModule
import org.metaborg.core.editor.{IEditorRegistry, NullEditorRegistry}
import org.metaborg.spoofax.core.SpoofaxModule

class FragmentsModule extends SpoofaxModule with ScalaModule {
  // Only needed in CLI to prevent a warning/error in output
  override def bindEditor() {
    bind[IEditorRegistry].to[NullEditorRegistry].in[Singleton]
  }
}
