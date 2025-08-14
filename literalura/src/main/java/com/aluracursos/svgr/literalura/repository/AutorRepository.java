package com.aluracursos.svgr.literalura.repository;

import com.aluracursos.svgr.literalura.model.Autor;
import com.aluracursos.svgr.literalura.model.Idioma;
import com.aluracursos.svgr.literalura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutorRepository extends JpaRepository<Autor, Long> {
    Optional<Autor> buscarAutorPorNombre(@Param("nombre") String nombre);

    @Query("SELECT l FROM Libro l WHERE l.titulo LIKE %:titulo%")
    Optional<Libro> buscarLibroPorTitulo(@Param("titulo") String titulo);

    @Query("SELECT l FROM libro l ORDER BY l.titulo")
    List<Libro> librosRegistrados();

    @Query("SELECT a FROM Autor a WHERE a.fechaDeNacimiento <= :fecha AND a.fechaDeFallecimiento > :fecha")
    List<Autor> listarAutoresVivos(@Param("fecha") Integer fecha);

    @Query("SELECT l FROM libro l WHERE l.idiomas = :idioma")
    List<Libro> librosPorIdioma(@Param("idioma") Idioma idioma);
}


