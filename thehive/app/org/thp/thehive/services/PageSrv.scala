package org.thp.thehive.services

import gremlin.scala.{Graph, GremlinScala, Vertex}
import javax.inject.{Inject, Singleton}
import org.thp.scalligraph.EntitySteps
import org.thp.scalligraph.auth.AuthContext
import org.thp.scalligraph.models.{Database, Entity}
import org.thp.scalligraph.services.{EdgeSrv, VertexSrv}
import org.thp.scalligraph.steps.StepsOps._
import org.thp.scalligraph.steps.VertexSteps
import org.thp.thehive.models.{Organisation, OrganisationPage, Page}
import play.api.libs.json.Json

import scala.util.Try

@Singleton
class PageSrv @Inject()(implicit db: Database, organisationSrv: OrganisationSrv, auditSrv: AuditSrv) extends VertexSrv[Page, PageSteps] {

  val organisationPageSrv = new EdgeSrv[OrganisationPage, Organisation, Page]

  override def steps(raw: GremlinScala[Vertex])(implicit graph: Graph): PageSteps = new PageSteps(raw)

  def create(page: Page)(implicit authContext: AuthContext, graph: Graph): Try[Page with Entity] =
    for {
      created      <- createEntity(page)
      organisation <- organisationSrv.get(authContext.organisation).getOrFail()
      _            <- organisationPageSrv.create(OrganisationPage(), organisation, created)
      _            <- auditSrv.page.create(created, Json.obj("title" -> page.title))
    } yield created

}

@EntitySteps[Page]
class PageSteps(raw: GremlinScala[Vertex])(implicit db: Database, graph: Graph) extends VertexSteps[Page](raw) {
  override def newInstance(newRaw: GremlinScala[Vertex]): PageSteps = new PageSteps(newRaw)
  override def newInstance(): PageSteps                             = new PageSteps(raw.clone())

  def getByTitle(title: String): PageSteps = this.has("title", title)

  def visible(implicit authContext: AuthContext): PageSteps = this.filter(
    _.inTo[OrganisationPage]
      .has("name", authContext.organisation)
  )
}
