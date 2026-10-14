package com.fiteasy.shared
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable data class RigDocument(val format:String="fiteasy-rig",val version:Int=1,val clip:RigClip)
fun exportRig(clip:RigClip):ByteArray {validateRig(clip);return codec.encodeToString(RigDocument(clip=clip)).toByteArray(Charsets.UTF_8)}
fun importRig(bytes:ByteArray):RigClip {require(bytes.size in 1..1_048_576);val doc=codec.decodeFromString<RigDocument>(bytes.toString(Charsets.UTF_8));require(doc.format=="fiteasy-rig" && doc.version==1);validateRig(doc.clip);return doc.clip}
/** Editable vector pose preview; JSON is the lossless animation round trip. */
fun exportRigSvg(clip:RigClip,pose:RigPose):ByteArray {
 validateRig(clip)
 val points=rigJoints.associateWith {rigProject(rigWorldPoint(pose,it),pose.yaw,pose.pitch)}
 fun escape(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace("\"","&quot;")
 val body=rigBones.joinToString(""){(a,b)->val x=points.getValue(a);val y=points.getValue(b);"<line id=\"$b\" x1=\"${x.x}\" y1=\"${x.y}\" x2=\"${y.x}\" y2=\"${y.y}\"/>"}
 val labels=clip.props.filter {it.name.isNotBlank()}.joinToString(""){p->val q=rigProject(p.point,pose.yaw,pose.pitch);"<text x=\"${q.x}\" y=\"${q.y}\" font-size=\"4\" fill=\"black\" stroke=\"none\">${escape(p.name)}</text>"}
 return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"-50 -10 200 120\"><title>FitEasy rig pose</title><g fill=\"none\" stroke=\"#168b70\" stroke-width=\"2\" stroke-linecap=\"round\">$body$labels</g></svg>".toByteArray(Charsets.UTF_8)
}
