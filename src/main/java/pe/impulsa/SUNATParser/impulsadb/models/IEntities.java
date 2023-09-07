package pe.impulsa.SUNATParser.impulsadb.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name="entities",schema="acc")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@DynamicUpdate
public class IEntities {
    /*@GeneratedValue(strategy = GenerationType.IDENTITY)
            TABLE,
            generator = "table-generator")
    @TableGenerator(name = "table-generator",
            schema="acc",
            table = "entities",
            pkColumnName = "entity_id",
            valueColumnName = "seq_value")*/
    @Id
    @Column(name = "ruc")
    private Long ruc;
    @Basic
    @Column(name="nombre_razon")
    private String nombreRazon;
    @Basic
    @Column(name="usuario_sol")
    private String usuarioSol;
    @Basic
    @Column(name="clave_sol")
    private String claveSol;
    @Basic
    @Column(name="observaciones")
    private String observaciones;
    @Basic
    @Column(name="activo")
    private Boolean activo;
    @Basic
    @Column(name="alias")
    private String alias;
    @Basic
    @Column(name="related_user")
    private Long related_user;
    @Basic
    @Column(name="suscribed_until")
    private Boolean suscribedUntil;
    @Basic
    @Column(name="suscription")
    private Integer suscription;
}