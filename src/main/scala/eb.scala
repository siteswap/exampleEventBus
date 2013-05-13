import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef
import akka.routing.BroadcastRouter
import akka.event.ActorEventBus
import scala.concurrent.duration._
import akka.event.EventBus
import akka.event.Logging
// import akka.util.cps._ // content pub sub?

object eb extends App {
  
  sealed trait DataItem // Create message taxonomy
  case class MarketData(name:String, d:Array[Int]) extends DataItem {
    override def toString() : String = {
      return(name + ": " + d)
    }
  }
  case class SignalData(name:String, d:Array[Int]) extends DataItem
  case class Command(name:String) extends DataItem
  // case means we dont have to use 'new' and sealed means we get some compile time checks that we have caught all possibilities
 
  
  val system = ActorSystem("EventBus")
  // TODO - what is 'execution context' ?
  import system.dispatcher
  val prod = system.actorOf(Props[Producer], name = "anOptionalNameForMyActor") // Creates type "ActorRef" - ActorRef has '!' method
  val cancellable = system.scheduler.schedule( 0 milliseconds, 1000 milliseconds, prod, Command("Clock")) // or pass the class 'Command'
  // ActorRef is *network aware* -> you can serialize/send over network and still it refers to the same actor!
 
  
  class Consumer extends Actor {
    
    // By default, we are just using the default "akka.event.Logging$DefaultLogger"
    // TODO - change this to log to database
    val log = Logging(context.system, this) // Which system EventStream to use for logging
    
    // Use the context object to get hold of the actor's actorSystem, then use the standard eventStream
    context.system.eventStream.subscribe(context.self, classOf[MarketData])
    
    def receive = {
      case d:DataItem => log.info( d.toString() )
      case _ => log.info("Consumer: not recognised")
    }
  }
  
  class Producer extends Actor {
    
    val log = Logging(context.system, this)
    val consumer = context.actorOf( Props[Consumer].withRouter( BroadcastRouter(3) ) )
    
    // TODO - arent we supposed to be guaranteed a check here for sealed classes?
    def receive = {
      case Command("Clock") => system.eventStream.publish(MarketData("VOD.L",Array[Int](3,2,4)))
      case x:Any => log.info("Producer: not recognised " + x)
   }
    
  }
  
  
  
}