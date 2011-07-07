package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class SelfReferencingOneToManyTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class SelfReferencingOneToManyCategory {
    String name

    static hasMany = [children: SelfReferencingOneToManyCategory]
    static belongsTo = [parent: SelfReferencingOneToManyCategory]

    static constraints = {
        parent(nullable: true)
    }
}
'''
    }

    void testCascadingDeletes() {
        def categoryClass = ga.getDomainClass("SelfReferencingOneToManyCategory").clazz

        assertNotNull categoryClass.newInstance(name:"Root")
                                   .addToChildren(name:"Child 1")
                                   .addToChildren(name:"Child 2")
                                   .save(flush:true)

        session.clear()

        def category = categoryClass.get(1)
        def child = category.children.find { it.name == 'Child 1' }

        category.removeFromChildren(child)
        child.delete(flush:true)

        session.clear()

        category = categoryClass.get(1)
        assertNotNull category
        assertEquals 1, category.children.size()
    }

    void testThreeLevelCascadingDeleteToChildren() {

        def categoryClass = ga.getDomainClass("SelfReferencingOneToManyCategory").clazz

        def root = categoryClass.newInstance(name:"Root")

        def child1 = categoryClass.newInstance(name:"Child 1")
                                  .addToChildren(name:"Second Level Child 1")
                                  .addToChildren(name:"Second Level Child 2")

        def child2 = categoryClass.newInstance(name:"Child 2")
                                  .addToChildren(name:"Second Level Child 1")
                                  .addToChildren(name:"Second Level Child 2")

        root.addToChildren(child1)
        root.addToChildren(child2)

        root.save(flush:true)

        session.clear()

        def category = categoryClass.get(1)
        def child = category.children.find { it.name == 'Child 1' }

        assertEquals 2, child.children.size()
        assertEquals category, child.parent

        category.removeFromChildren(child)
        child.delete(flush:true)

        session.clear()

        category = categoryClass.get(1)
        assertNotNull category
        assertEquals 1, category.children.size()
    }
}
