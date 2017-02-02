package org.metaborg.spg.cmd

import com.google.inject.Singleton
import net.codingwell.scalaguice.ScalaModule
import org.metaborg.core.editor.{IEditorRegistry, NullEditorRegistry}
import org.metaborg.spoofax.core.SpoofaxModule

class SPGModule extends SpoofaxModule with ScalaModule {
  override def bindEditor() {
    bind[IEditorRegistry].to[NullEditorRegistry].in[Singleton]
  }
}
