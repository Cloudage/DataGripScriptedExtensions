import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

typeMapping = [
  (~/(?i)int/)                      : "Integer",
  (~/(?i)float|double|decimal|real/): "Double",
  (~/(?i)numeric|decimal/)          : "java.math.BigDecimal",
  (~/(?i)datetime|timestamp/)       : "java.util.Date",
  (~/(?i)date/)                     : "java.sql.Date",
  (~/(?i)time/)                     : "java.sql.Date",
  (~/(?i)boolean/)                  : "Boolean",
  (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".java").withPrintWriter { out -> generate(out, dir, table, className, fields) }
}

def generate(out, dir, table, className, fields) {
  def tableName = table.getName()
  def tableComment = table.getComment()

  def packageName = dir.toString().find(/\/src\/main\/java\/(.*)/).substring(15).replace("/",".")

  out.println "package $packageName;"
  out.println "import io.swagger.annotations.ApiModel;"
  out.println "import io.swagger.annotations.ApiModelProperty;"
  out.println "import javax.persistence.*;"
  out.println ""
  out.println ""
  out.println "@ApiModel(value=\"$className\",description = \"${tableComment}\")"
  out.println "@Table(name = \"$tableName\")"
  out.println "@Entity"
  out.println "public class $className {"
  out.println ""
  fields.each() {
    if(it.comment != ""){
      out.println "  /** ${it.comment} */"
      out.println "  @ApiModelProperty(\"${it.comment}\")"
    }

    if(it.isId) {
      out.println "  @Id"
      out.println "  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = \"${tableName}_seq_gen\")"
      out.println "  @SequenceGenerator(name = \"${tableName}_seq_gen\", sequenceName = \"${tableName}_id_seq\")"
    }

    out.println "  @Column(name = \"${it.column}\")"
    out.println "  private ${it.type} ${it.name};"
    out.println ""
  }

  out.println ""
  out.println "  // Setters =======================================//"

  fields.each() {
    if(it.comment != "") out.println "  /** ${it.comment} */"
    out.println "  public $className set${it.name.capitalize()}(${it.type} ${it.name}) {"
    out.println "    this.${it.name} = ${it.name};"
    out.println "    return this;"
    out.println "  }"
    out.println ""
  }

  out.println ""
  out.println "  // Getters =======================================//"

  fields.each() {
    if(it.comment != "") out.println "  /** ${it.comment} */"
    out.println "  public ${it.type} get${it.name.capitalize()}() {"
    out.println "    return ${it.name};"
    out.println "  }"
    out.println ""
  }

  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                 name : javaName(col.getName(), false),
                 column : col.getName(),
                 type : typeStr,
                 comment: col.getComment(),
                 isId: DasUtil.isPrimary(col)]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
