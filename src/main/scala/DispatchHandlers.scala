package bintray

import dispatch.FunctionHandler

trait DispatchHandlers {
  def asStatusAndBody =
    new FunctionHandler({ r => (r.getStatusCode, r.getResponseBody)})

  def asCreated =
    new FunctionHandler(_.getStatusCode == 201)

  def asFound = 
    new FunctionHandler(_.getStatusCode != 404)    
}
