package org.thp.thehive.controllers.v0

import javax.inject.{Inject, Named, Singleton}
import org.thp.scalligraph.controllers.{Entrypoint, FieldsParser}
import org.thp.scalligraph.models.{Database, Entity}
import org.thp.scalligraph.query.{ParamQuery, PublicProperty, Query}
import org.thp.scalligraph.traversal.TraversalOps._
import org.thp.scalligraph.traversal.{IteratorOutput, Traversal}
import org.thp.thehive.controllers.v0.Conversion._
import org.thp.thehive.dto.v0.InputObservableType
import org.thp.thehive.models.{ObservableType, Permissions}
import org.thp.thehive.services.ObservableTypeSrv
import play.api.mvc.{Action, AnyContent, Results}

@Singleton
class ObservableTypeCtrl @Inject() (
    entrypoint: Entrypoint,
    @Named("with-thehive-schema") db: Database,
    properties: Properties,
    observableTypeSrv: ObservableTypeSrv
) extends QueryableCtrl {

  override val entityName: String                           = "ObjservableType"
  override val publicProperties: List[PublicProperty[_, _]] = properties.observableType
  override val initialQuery: Query =
    Query.init[Traversal.V[ObservableType]]("listObservableType", (graph, _) => observableTypeSrv.startTraversal(graph))
  override val pageQuery: ParamQuery[OutputParam] =
    Query.withParam[OutputParam, Traversal.V[ObservableType], IteratorOutput](
      "page",
      FieldsParser[OutputParam],
      (range, observableTypeSteps, _) => observableTypeSteps.richPage(range.from, range.to, withTotal = true)(identity)
    )
  override val outputQuery: Query = Query.output[ObservableType with Entity]
  override val getQuery: ParamQuery[IdOrName] = Query.initWithParam[IdOrName, Traversal.V[ObservableType]](
    "getObservableType",
    FieldsParser[IdOrName],
    (param, graph, _) => observableTypeSrv.get(param.idOrName)(graph)
  )

  def get(idOrName: String): Action[AnyContent] = entrypoint("get observable type").authRoTransaction(db) { _ => implicit graph =>
    observableTypeSrv
      .get(idOrName)
      .getOrFail("Observable")
      .map(ot => Results.Ok(ot.toJson))
  }

  def create: Action[AnyContent] =
    entrypoint("create observable type")
      .extract("observableType", FieldsParser[InputObservableType])
      .authPermittedTransaction(db, Permissions.manageObservableTemplate) { implicit request => implicit graph =>
        val inputObservableType: InputObservableType = request.body("observableType")
        observableTypeSrv
          .create(inputObservableType.toObservableType)
          .map(observableType => Results.Created(observableType.toJson))
      }

  def delete(idOrName: String): Action[AnyContent] =
    entrypoint("delete observable type")
      .authPermittedTransaction(db, Permissions.manageObservableTemplate) { _ => implicit graph =>
        observableTypeSrv.remove(idOrName).map(_ => Results.NoContent)
      }
}