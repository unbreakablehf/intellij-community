package com.intellij.workspaceModel.storage.entities.test.api

import com.intellij.workspaceModel.storage.*
import com.intellij.workspaceModel.storage.EntityInformation
import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.EntityStorage
import com.intellij.workspaceModel.storage.GeneratedCodeApiVersion
import com.intellij.workspaceModel.storage.GeneratedCodeImplVersion
import com.intellij.workspaceModel.storage.ModifiableWorkspaceEntity
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntity
import com.intellij.workspaceModel.storage.impl.ConnectionId
import com.intellij.workspaceModel.storage.impl.EntityLink
import com.intellij.workspaceModel.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.workspaceModel.storage.impl.WorkspaceEntityBase
import com.intellij.workspaceModel.storage.impl.WorkspaceEntityData
import com.intellij.workspaceModel.storage.impl.extractOneToManyChildren
import com.intellij.workspaceModel.storage.impl.extractOneToManyParent
import com.intellij.workspaceModel.storage.impl.updateOneToManyChildrenOfParent
import com.intellij.workspaceModel.storage.impl.updateOneToManyParentOfChild
import org.jetbrains.deft.ObjBuilder
import org.jetbrains.deft.Type
import org.jetbrains.deft.annotations.Child

@GeneratedCodeApiVersion(1)
@GeneratedCodeImplVersion(1)
open class XChildEntityImpl: XChildEntity, WorkspaceEntityBase() {
    
    companion object {
        internal val PARENTENTITY_CONNECTION_ID: ConnectionId = ConnectionId.create(XParentEntity::class.java, XChildEntity::class.java, ConnectionId.ConnectionType.ONE_TO_MANY, false)
        internal val CHILDCHILD_CONNECTION_ID: ConnectionId = ConnectionId.create(XChildEntity::class.java, XChildChildEntity::class.java, ConnectionId.ConnectionType.ONE_TO_MANY, false)
        
        val connections = listOf<ConnectionId>(
            PARENTENTITY_CONNECTION_ID,
            CHILDCHILD_CONNECTION_ID,
        )

    }
        
    @JvmField var _childProperty: String? = null
    override val childProperty: String
        get() = _childProperty!!
                        
    @JvmField var _dataClass: DataClassX? = null
    override val dataClass: DataClassX?
        get() = _dataClass
                        
    override val parentEntity: XParentEntity
        get() = snapshot.extractOneToManyParent(PARENTENTITY_CONNECTION_ID, this)!!           
        
    override val childChild: List<XChildChildEntity>
        get() = snapshot.extractOneToManyChildren<XChildChildEntity>(CHILDCHILD_CONNECTION_ID, this)!!.toList()
    
    override fun connectionIdList(): List<ConnectionId> {
        return connections
    }

    class Builder(val result: XChildEntityData?): ModifiableWorkspaceEntityBase<XChildEntity>(), XChildEntity.Builder {
        constructor(): this(XChildEntityData())
        
        override fun applyToBuilder(builder: MutableEntityStorage) {
            if (this.diff != null) {
                if (existsInBuilder(builder)) {
                    this.diff = builder
                    return
                }
                else {
                    error("Entity XChildEntity is already created in a different builder")
                }
            }
            
            this.diff = builder
            this.snapshot = builder
            addToBuilder()
            this.id = getEntityData().createEntityId()
            
            // Process linked entities that are connected without a builder
            processLinkedEntities(builder)
            checkInitialization() // TODO uncomment and check failed tests
        }
    
        fun checkInitialization() {
            val _diff = diff
            if (!getEntityData().isChildPropertyInitialized()) {
                error("Field XChildEntity#childProperty should be initialized")
            }
            if (!getEntityData().isEntitySourceInitialized()) {
                error("Field XChildEntity#entitySource should be initialized")
            }
            if (_diff != null) {
                if (_diff.extractOneToManyParent<WorkspaceEntityBase>(PARENTENTITY_CONNECTION_ID, this) == null) {
                    error("Field XChildEntity#parentEntity should be initialized")
                }
            }
            else {
                if (this.entityLinks[EntityLink(false, PARENTENTITY_CONNECTION_ID)] == null) {
                    error("Field XChildEntity#parentEntity should be initialized")
                }
            }
            // Check initialization for list with ref type
            if (_diff != null) {
                if (_diff.extractOneToManyChildren<WorkspaceEntityBase>(CHILDCHILD_CONNECTION_ID, this) == null) {
                    error("Field XChildEntity#childChild should be initialized")
                }
            }
            else {
                if (this.entityLinks[EntityLink(true, CHILDCHILD_CONNECTION_ID)] == null) {
                    error("Field XChildEntity#childChild should be initialized")
                }
            }
        }
        
        override fun connectionIdList(): List<ConnectionId> {
            return connections
        }
    
        
        override var childProperty: String
            get() = getEntityData().childProperty
            set(value) {
                checkModificationAllowed()
                getEntityData().childProperty = value
                changedProperty.add("childProperty")
            }
            
        override var entitySource: EntitySource
            get() = getEntityData().entitySource
            set(value) {
                checkModificationAllowed()
                getEntityData().entitySource = value
                changedProperty.add("entitySource")
                
            }
            
        override var dataClass: DataClassX?
            get() = getEntityData().dataClass
            set(value) {
                checkModificationAllowed()
                getEntityData().dataClass = value
                changedProperty.add("dataClass")
                
            }
            
        override var parentEntity: XParentEntity
            get() {
                val _diff = diff
                return if (_diff != null) {
                    _diff.extractOneToManyParent(PARENTENTITY_CONNECTION_ID, this) ?: this.entityLinks[EntityLink(false, PARENTENTITY_CONNECTION_ID)]!! as XParentEntity
                } else {
                    this.entityLinks[EntityLink(false, PARENTENTITY_CONNECTION_ID)]!! as XParentEntity
                }
            }
            set(value) {
                checkModificationAllowed()
                val _diff = diff
                if (_diff != null && value is ModifiableWorkspaceEntityBase<*> && value.diff == null) {
                    // Setting backref of the list
                    if (value is ModifiableWorkspaceEntityBase<*>) {
                        val data = (value.entityLinks[EntityLink(true, PARENTENTITY_CONNECTION_ID)] as? List<Any> ?: emptyList()) + this
                        value.entityLinks[EntityLink(true, PARENTENTITY_CONNECTION_ID)] = data
                    }
                    // else you're attaching a new entity to an existing entity that is not modifiable
                    _diff.addEntity(value)
                }
                if (_diff != null && (value !is ModifiableWorkspaceEntityBase<*> || value.diff != null)) {
                    _diff.updateOneToManyParentOfChild(PARENTENTITY_CONNECTION_ID, this, value)
                }
                else {
                    // Setting backref of the list
                    if (value is ModifiableWorkspaceEntityBase<*>) {
                        val data = (value.entityLinks[EntityLink(true, PARENTENTITY_CONNECTION_ID)] as? List<Any> ?: emptyList()) + this
                        value.entityLinks[EntityLink(true, PARENTENTITY_CONNECTION_ID)] = data
                    }
                    // else you're attaching a new entity to an existing entity that is not modifiable
                    
                    this.entityLinks[EntityLink(false, PARENTENTITY_CONNECTION_ID)] = value
                }
                changedProperty.add("parentEntity")
            }
        
        // List of non-abstract referenced types
        var _childChild: List<XChildChildEntity>? = emptyList()
        override var childChild: List<XChildChildEntity>
            get() {
                // Getter of the list of non-abstract referenced types
                val _diff = diff
                return if (_diff != null) {
                    _diff.extractOneToManyChildren<XChildChildEntity>(CHILDCHILD_CONNECTION_ID, this)!!.toList() + (this.entityLinks[EntityLink(true, CHILDCHILD_CONNECTION_ID)] as? List<XChildChildEntity> ?: emptyList())
                } else {
                    this.entityLinks[EntityLink(true, CHILDCHILD_CONNECTION_ID)] as? List<XChildChildEntity> ?: emptyList()
                }
            }
            set(value) {
                // Setter of the list of non-abstract referenced types
                checkModificationAllowed()
                val _diff = diff
                if (_diff != null) {
                    for (item_value in value) {
                        if (item_value is ModifiableWorkspaceEntityBase<*> && (item_value as? ModifiableWorkspaceEntityBase<*>)?.diff == null) {
                            _diff.addEntity(item_value)
                        }
                    }
                    _diff.updateOneToManyChildrenOfParent(CHILDCHILD_CONNECTION_ID, this, value)
                }
                else {
                    for (item_value in value) {
                        if (item_value is ModifiableWorkspaceEntityBase<*>) {
                            item_value.entityLinks[EntityLink(false, CHILDCHILD_CONNECTION_ID)] = this
                        }
                        // else you're attaching a new entity to an existing entity that is not modifiable
                    }
                    
                    this.entityLinks[EntityLink(true, CHILDCHILD_CONNECTION_ID)] = value
                }
                changedProperty.add("childChild")
            }
        
        override fun getEntityData(): XChildEntityData = result ?: super.getEntityData() as XChildEntityData
        override fun getEntityClass(): Class<XChildEntity> = XChildEntity::class.java
    }
}
    
class XChildEntityData : WorkspaceEntityData<XChildEntity>() {
    lateinit var childProperty: String
    var dataClass: DataClassX? = null

    fun isChildPropertyInitialized(): Boolean = ::childProperty.isInitialized

    override fun wrapAsModifiable(diff: MutableEntityStorage): ModifiableWorkspaceEntity<XChildEntity> {
        val modifiable = XChildEntityImpl.Builder(null)
        modifiable.allowModifications {
          modifiable.diff = diff
          modifiable.snapshot = diff
          modifiable.id = createEntityId()
          modifiable.entitySource = this.entitySource
        }
        modifiable.changedProperty.clear()
        return modifiable
    }

    override fun createEntity(snapshot: EntityStorage): XChildEntity {
        val entity = XChildEntityImpl()
        entity._childProperty = childProperty
        entity._dataClass = dataClass
        entity.entitySource = entitySource
        entity.snapshot = snapshot
        entity.id = createEntityId()
        return entity
    }

    override fun getEntityInterface(): Class<out WorkspaceEntity> {
        return XChildEntity::class.java
    }

    override fun serialize(ser: EntityInformation.Serializer) {
    }

    override fun deserialize(de: EntityInformation.Deserializer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this::class != other::class) return false
        
        other as XChildEntityData
        
        if (this.childProperty != other.childProperty) return false
        if (this.entitySource != other.entitySource) return false
        if (this.dataClass != other.dataClass) return false
        return true
    }

    override fun equalsIgnoringEntitySource(other: Any?): Boolean {
        if (other == null) return false
        if (this::class != other::class) return false
        
        other as XChildEntityData
        
        if (this.childProperty != other.childProperty) return false
        if (this.dataClass != other.dataClass) return false
        return true
    }

    override fun hashCode(): Int {
        var result = entitySource.hashCode()
        result = 31 * result + childProperty.hashCode()
        result = 31 * result + dataClass.hashCode()
        return result
    }
}