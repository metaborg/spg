package org.metaborg.spg

import javax.inject.Singleton

import net.codingwell.scalaguice.ScalaModule
import org.metaborg.core.editor.{IEditorRegistry, NullEditorRegistry}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.SpoofaxModule

class FragmentsModule extends SpoofaxModule with ScalaModule {
  override def bindProject() {
    bind[SimpleProjectService].in[Singleton]
    bind[IProjectService].to[SimpleProjectService]
  }

  override def bindEditor() {
    bind[IEditorRegistry].to[NullEditorRegistry].in[Singleton]
  }
}
