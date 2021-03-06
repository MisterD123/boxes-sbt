package boxes.swing

import java.awt.event.ActionEvent
import boxes.list.{ListMultiDeleteOp, ListDeleteOp, ListMultiAddOp, ListAddOp, ListMoveOp, ListMultiMoveOp}
import javax.swing._
import border.EmptyBorder
import com.explodingpixels.swingx.EPPanel
import java.awt.{BorderLayout, Component}
import boxes.swing.icons.IconFactory
import boxes._

//TODO should make an ExtendedOp that has a name:Ref[String] and icon:Ref[Icon] (not sure about
//icon, maybe make return an image?, and an Action that is a view of these.
class SwingOpAction(name:String, icon:Option[Icon], op:Op) extends AbstractAction(name, icon.getOrElse(null)) {
  def actionPerformed(e:ActionEvent) {
    op()
  }
  val view = View {
    val enabled = op.canApply()
    SwingView.replaceUpdate(
      this,
      {
        setEnabled(enabled)
      }
    )
  }
}

object SwingOp {

  val add = Some(IconFactory.icon("Plus"))
  val delete = Some(IconFactory.icon("Minus"))
  val up = Some(IconFactory.icon("Up"))
  val down = Some(IconFactory.icon("Down"))

  def apply(name:String = "", icon:Option[Icon] = None, op:Op):SwingOpAction = new SwingOpAction(name, icon, op)

  def apply(op:Op):SwingOpAction = {
    op match {
      case o:ListAddOp[_] => SwingOp("", add, op)
      case o:ListMultiAddOp[_] => SwingOp("", add, op)
      case o:ListDeleteOp[_] => SwingOp("", delete, op)
      case o:ListMultiDeleteOp[_] => SwingOp("", delete, op)
      case o:ListMoveOp[_] => {
        if (o.up) {
          SwingOp("", up, op)
        } else {
          SwingOp("", down, op)
        }
      }
      case o:ListMultiMoveOp[_] => {
        if (o.up) {
          SwingOp("", up, op)
        } else {
          SwingOp("", down, op)
        }
      }
      //FIXME use implicits
      case _ => throw new IllegalArgumentException("Unknown op")
    }
  }

}

object SwingBarPadding {
  def apply() = {
    val panel = new EPPanel()
    panel.setBackgroundPainter(BarStylePainter[Component](false, false))
    panel
  }
}

object SwingButtonBar {
  def apply() = new SwingButtonBarBuilder(List[JComponent]())
}

class SwingButtonBarBuilder(val components:List[JComponent]) {
  def add(c:JComponent) = new SwingButtonBarBuilder(components ::: List(c))
  def add(op:Op):SwingButtonBarBuilder = add(SwingBarButton(op))
  def add(v:SwingView) = new SwingButtonBarBuilder(components ::: List(v.component))

  def addComponent(c:Option[JComponent]) = c.map(cv => new SwingButtonBarBuilder(components ::: List(cv))).getOrElse(this)
  def addOp(op:Option[Op]):SwingButtonBarBuilder = addComponent(op.map(SwingBarButton(_)))
  def addSwingView(v:Option[SwingView]): SwingButtonBarBuilder = addComponent(v.map(_.component))

  def buildWithListStyleComponent(c:JComponent) = {
    val padding = SwingBarPadding()
    padding.setBorder(new EmptyBorder(0, 5, 0, 5))
    padding.setLayout(new BorderLayout)
    padding.add(c)
    c.setOpaque(false)
    build(padding)
  }

  def build(padding:JComponent = SwingBarPadding()) = {
    val buttonPanel = new JPanel()
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS))
    components.foreach(c => buttonPanel.add(c))

    val bottom = new JPanel(new BorderLayout())
    bottom.add(buttonPanel, BorderLayout.WEST)
    bottom.add(padding, BorderLayout.CENTER)

    bottom
  }
}





