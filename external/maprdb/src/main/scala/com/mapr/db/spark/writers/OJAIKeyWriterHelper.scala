/* Copyright (c) 2015 & onwards. MapR Tech, Inc., All rights reserved */
package com.mapr.db.spark.writers

import com.mapr.db.spark.codec.BeanCodec
import com.mapr.db.spark.condition.DBQueryCondition
import com.mapr.db.spark.dbclient.DBClient
import org.ojai.{Document, Value}
import com.mapr.db.spark.impl.OJAIDocument
import org.ojai.store.DocumentMutation

private[spark] sealed trait OJAIValue[T] extends Serializable {
  type Self
  def getValue(elem: T): Document
  def write(doc: Document, getID: (Document) => Value, writer: Writer)
  def update(mutation: DocumentMutation, getID: Value, writer: TableUpdateWriter)
  def checkAndUpdate(mutation: DocumentMutation, queryCondition: DBQueryCondition, getID: Value, writer: TableCheckAndMutateWriter)
}

private[spark] object OJAIValue extends BaseOJAIValue {

  implicit def defaultOJAIDocument[T]: OJAIValue[OJAIDocument] = new OJAIValue[OJAIDocument] {
    type Self = OJAIDocument
    override def getValue(elem: OJAIDocument): Document = elem.getDoc
    override def write(doc: Document, getID: (Document)=> Value, writer: Writer) = writer.write(doc, getID(doc))
    override def update(mutation: DocumentMutation, getID: Value, writer: TableUpdateWriter) = writer.write(mutation, getID)
    override def checkAndUpdate(mutation: DocumentMutation, queryCondition: DBQueryCondition, getID: Value, writer: TableCheckAndMutateWriter): Unit =
      writer.write(mutation, queryCondition, getID)
  }
}

private[spark] trait BaseOJAIValue {
  implicit def overrideDefault[T <: AnyRef]: OJAIValue[T] = new OJAIValue[T] {
    type Self = AnyRef
    override def getValue(elem: T): Document  = BeanCodec.decode(DBClient().newDocumentBuilder(), elem)
    override def write(doc: Document, getID: (Document) => Value, writer: Writer) = writer.write(doc, getID(doc))
    override def update(mutation: DocumentMutation, getID: Value, writer: TableUpdateWriter) = writer.write(mutation, getID)
    override def checkAndUpdate(mutation: DocumentMutation, queryCondition: DBQueryCondition, getID: Value, writer: TableCheckAndMutateWriter): Unit =
      writer.write(mutation, queryCondition, getID)
  }

  def overrideJavaDefault[T <: AnyRef]: OJAIValue[T] = new OJAIValue[T] {
    type Self = AnyRef
    override def getValue(elem: T): Document  = org.ojai.beans.BeanCodec.decode(DBClient().newDocumentBuilder(), elem)
    override def write(doc: Document, getID: (Document) => Value, writer: Writer) = writer.write(doc, getID(doc))
    override def update(mutation: DocumentMutation, getID: Value, writer: TableUpdateWriter) = writer.write(mutation, getID)
    override def checkAndUpdate(mutation: DocumentMutation, queryCondition: DBQueryCondition, getID: Value, writer: TableCheckAndMutateWriter): Unit =
      writer.write(mutation, queryCondition, getID)
  }
}
