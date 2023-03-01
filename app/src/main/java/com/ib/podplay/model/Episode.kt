package com.ib.podplay.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*


@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]

/*
* entity:Defines the parent entity

* parentColumns:Defines the column names on the parent entity(the podcast class)

* childColumns:Defines the column names in the child entity(the Episode class)

* onDelete:Defines the behavior when the parent entity is deleted.CASCADE
*
* indicates that any time you delete a podcast, all related child episodes are deleted automatically.
*/
)

data class Episode (
    @PrimaryKey var guid:String ="",
    var podcastId:Long? = null,
    var title: String ="",
    var description:String ="",
    var mediaUrl: String ="",
    var mimeType:String = "",
    var releaseDate:Date = Date(),
    var duration: String ="",
    var lastPosition:Long? = null
):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(podcastId.toString())
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(mediaUrl)
        dest.writeString(mimeType)
        dest.writeString(releaseDate.toString())
        dest.writeString(duration)
        dest.writeString(lastPosition.toString())
    }

    companion object CREATOR : Parcelable.Creator<Episode> {
        override fun createFromParcel(source: Parcel): Episode {
            return Episode(source)
        }

        override fun newArray(size: Int): Array<Episode?> {
            return arrayOfNulls(size)
        }
    }
}

/*
* guid:Unique identifier provided in the RSS feed for an episode
* title:The name of the episode.
* description: Episode description
* mediaUrl:The location of the podcast episode
* mimeType:Determines the type of file located at mediaUrl
* releaseDate:The date the episode was release
* duration:Length of the episode provided by the RSS feed.
* */


