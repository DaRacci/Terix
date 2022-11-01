package dev.racci.terix.core.utils

import org.checkerframework.common.value.qual.IntRange
import org.incendo.interfaces.core.element.Element
import org.incendo.interfaces.core.pane.GridPane
import org.incendo.interfaces.core.transform.InterfaceProperty
import org.incendo.interfaces.core.transform.ReactiveTransform
import org.incendo.interfaces.core.transform.types.PaginatedTransform
import org.incendo.interfaces.core.util.Vector2
import org.incendo.interfaces.core.view.InterfaceView
import org.incendo.interfaces.core.view.InterfaceViewer
import kotlin.math.ceil

public typealias TransformFunction<S, T, U> = (RenderablePaginatedTransform<S, T, U>) -> S?
public typealias RenderFunction<S, T, U> = (InterfaceView<T, U>) -> S

// TODO -> Static elements which are modified.
/** Custom implementation of [PaginatedTransform] that allows rendering elements as well as the buttons. */
public class RenderablePaginatedTransform<S : Element, T : GridPane<T, S>, U : InterfaceViewer>(
    private val min: Vector2,
    private val max: Vector2,
    private val elementsSupplier: () -> List<RenderFunction<S, T, U>>
) : ReactiveTransform<T, U, Int> {
    private val pageProperty = InterfaceProperty.of(0)
    private val dim: Vector2 = Vector2.at(max.x() - min.x() + 1, max.y() - min.y() + 1)
    private val pageSupplier: () -> Int
    private var backwardElementPosition = Vector2.at(-1, -1)
    private var forwardElementPosition = Vector2.at(-1, -1)
    private var backwardElementBuilder: TransformFunction<S, T, U> = { null }
    private var forwardElementBuilder: TransformFunction<S, T, U> = { null }

    init {
        // Calculate the number of available slots.
        val slots = dim.x() * dim.y()
        // Calculate the number of pages occupied by the elements.
        pageSupplier = {
            val elements = elementsSupplier()
            // Calculate the number of elements.
            val numberOfElements = elements.size
            ceil(numberOfElements.toDouble() / slots.toDouble()).toInt()
        }
    }

    /**
     * Constructs a new paginated transform.
     *
     * @param min      the coordinates for the minimum (inclusive) point where the elements are rendered
     * @param max      the coordinates for the maximum (inclusive) point where the elements are rendered
     * @param elements the elements
     */
    public constructor(
        min: Vector2,
        max: Vector2,
        elements: List<RenderFunction<S, T, U>>
    ) : this(min, max, { elements })

    override fun properties(): Array<InterfaceProperty<*>> {
        return arrayOf(pageProperty)
    }

    /**
     * Get the property containing the pagination's current page.
     *
     * @return an Integer InterfaceProperty
     */
    public fun pageProperty(): InterfaceProperty<Int> {
        return pageProperty
    }

    override fun apply(
        originalPane: T,
        view: InterfaceView<T, U>
    ): T {
        check(!(page() < 0 || page() > maxPage())) {
            String.format(
                "Page number is out of bounds. Must be in the range [%d, %d].",
                0,
                maxPage()
            )
        }
        val suppliedElements = elementsSupplier()
        // Pane that we're updating.
        var pane = originalPane
        // Calculate the number of available slots.
        val slots = dim.x() * dim.y()
        // Calculate the offset.
        val offset = slots * page()
        // Calculate the page elements.
        val elements = suppliedElements.subList(offset, suppliedElements.size)
        // Index used to reference the elements.
        var elementIndex = 0
        // Render the elements.
        for (y in min.y()..max.y()) {
            var x = min.x()
            while (x <= max.x() && elementIndex < elements.size) {
                pane = pane.element(elements[elementIndex](view), x, y)
                // Increment the element index.
                elementIndex++
                x++
            }
        }
        // Add the backward element, if one should exist.
        if (page() > 0) {
            val backwardElement = backwardElementBuilder(this)
            if (backwardElement != null) pane = pane.element(backwardElement, backwardElementPosition.x(), backwardElementPosition.y())
        }
        // Add the forward element, if one should exist.
        if (page() < maxPage()) {
            val forwardElement = forwardElementBuilder(this)
            if (forwardElement != null) pane = pane.element(forwardElement, forwardElementPosition.x(), forwardElementPosition.y())
        }
        // Return the updated pane.
        return pane
    }

    /**
     * Returns the current page number.
     *
     * @return the current page (0-indexed)
     */
    public fun page():
        @IntRange(from = 0)
        Int {
        return pageProperty.get()
    }

    /**
     * Returns the maximum page number.
     *
     * @return the maximum page (0-indexed)
     */
    public fun maxPage():
        @IntRange(from = 0)
        Int {
        return (pageSupplier() - 1).coerceAtLeast(0)
    }

    /**
     * Returns the number of pages.
     *
     * @return the number of pages
     */
    public fun pages():
        @IntRange(from = 1)
        Int {
        return pageSupplier()
    }

    /**
     * Returns the dimensions of the paginated view
     *
     * @return dimensions of the paginated view
     */
    public fun dimensions(): Vector2 {
        return dim
    }

    /**
     * Sets the backward element.
     *
     * @param position the position of the element
     * @param builder  the builder that builds the element
     */
    public fun backwardElement(
        position: Vector2,
        builder: TransformFunction<S, T, U>
    ) {
        backwardElementPosition = position
        backwardElementBuilder = builder
    }

    /**
     * Sets the forward element.
     *
     * @param position the position of the element
     * @param builder  the builder that builds the element
     */
    public fun forwardElement(
        position: Vector2,
        builder: TransformFunction<S, T, U>
    ) {
        forwardElementPosition = position
        forwardElementBuilder = builder
    }

    /**
     * Switch to the previous page.
     *
     * @throws IllegalStateException if the previous page does not exist
     */
    public fun previousPage() {
        check(page() != 0) {
            String.format(
                "Page number is out of bounds. Must be in the range [%d, %d].",
                0,
                maxPage()
            )
        }
        pageProperty.set(page() - 1)
    }

    /**
     * Switch to the next page.
     *
     * @throws IllegalStateException if the previous next does not exist
     */
    public fun nextPage() {
        check(page() != maxPage()) {
            String.format(
                "Page number is out of bounds. Must be in the range [%d, %d].",
                0,
                maxPage()
            )
        }
        pageProperty.set(page() + 1)
    }
}
