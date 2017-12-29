package com.lehvolk.xodus.web.resources

import com.fasterxml.jackson.core.JsonProcessingException
import com.lehvolk.xodus.web.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.StreamingOutput

class DatabaseResource(override val db: DBSummary) : DatabaseAwareResource() {

    @GET
    @Path("/types")
    fun searchEntities() = storeService.allTypes()

    @POST
    @Path("/types")
    fun searchEntities(type: String): Int = storeService.addType(type)

    @GET
    @Path("/entities")
    fun searchEntities(
            @QueryParam("id") id: Int,
            @QueryParam("q") term: String?,
            @QueryParam("offset") offset: Int = 0,
            @QueryParam("pageSize") pageSize: Int = 0): SearchPager {
        log.debug("searching entities by typeId: {}, term [{}] with offset = {} and pageSize = {}",
                id, term, offset, pageSize)
        if (offset < 0 || pageSize < 0) {
            throw BadRequestException()
        }
        safely {
            return storeService.searchType(id, term, offset, if (pageSize == 0) 50 else Math.min(pageSize, 1000))
        }
    }

    @GET
    @Path("/entities/{id}-{entityId}")
    fun getEntity(
            @PathParam("id") id: Int,
            @PathParam("entityId") entityId: Long): EntityView {
        log.debug("getting entity by typeId={} and entityId={}", id, entityId)
        safely {
            return storeService.getEntity(id, entityId)
        }
    }


    @PUT
    @Path("/entities/{id}-{entityId}")
    fun updateEntity(
            @PathParam("id") id: Int,
            @PathParam("entityId") entityId: Long,
            vo: ChangeSummary): EntityView {
        if (log.isDebugEnabled) {
            log.debug("updating entity for type {} and id {}. ChangeSummary: {}", id, entityId, toString(vo))
        }
        safely {
            return storeService.updateEntity(id, entityId, vo)
        }

    }

    @POST
    @Path("/entities/{id}")
    fun newEntity(
            @PathParam("id") id: Int,
            vo: ChangeSummary): EntityView {
        if (log.isDebugEnabled) {
            log.debug("creating entity for type {} and ChangeSummary: {}", id, toString(vo))
        }
        safely {
            return storeService.newEntity(id, vo)
        }
    }

    @DELETE
    @Path("/entities/{id}-{entityId}")
    fun deleteEntity(
            @PathParam("id") id: Int,
            @PathParam("entityId") entityId: Long) {
        log.debug("deleting entity for type {} and id {}", id, entityId)
        safely {
            storeService.deleteEntity(id, entityId)
        }
    }

    @GET
    @Path("/entities/{id}-{entityId}/blob/{blobName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM + ";charset=utf-8")
    fun getBlob(
            @PathParam("id") id: Int,
            @PathParam("entityId") entityId: Long,
            @PathParam("blobName") blobName: String): StreamingOutput {
        log.debug("getting entity blob data for type {} and id {} and blob '{}'", id, entityId, blobName)
        return StreamingOutput {
            safely {
                storeService.getBlob(id, entityId, blobName, it)
            }
        }
    }

    @GET
    @Path("/entities/{id}-{entityId}/links/{linkName}")
    fun getLinks(
            @PathParam("id") id: Int,
            @PathParam("entityId") entityId: Long,
            @PathParam("linkName") linkName: String,
            @QueryParam("offset") offset: Int = 0,
            @QueryParam("pageSize") pageSize: Int = 0): LinkPager {
        log.debug("searching entities by typeId: {}, entityId [{}], linkName [{}] with offset = {} and pageSize = {}",
                id, entityId, linkName, offset, pageSize)
        if (offset < 0 || pageSize < 0) {
            throw BadRequestException()
        }
        safely {
            return storeService.searchEntity(id, entityId, linkName, offset, if (pageSize == 0) 100 else Math.min(pageSize, 1000))
        }
    }

    private fun toString(vo: ChangeSummary): String {
        return try {
            configurator.mapper.writeValueAsString(vo)
        } catch (e: JsonProcessingException) {
            "Error converting vo to string. Check the server state this error should never happened"
        }
    }
}