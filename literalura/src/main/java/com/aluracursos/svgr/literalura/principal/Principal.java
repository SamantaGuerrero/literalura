package com.aluracursos.svgr.literalura.principal;

import com.aluracursos.svgr.literalura.model.*;
import com.aluracursos.svgr.literalura.service.ConsumoAPI;
import com.aluracursos.svgr.literalura.service.ConvierteDatos;
import org.springframework.stereotype.Component;
import com.aluracursos.svgr.literalura.repository.AutorRepository;
// import org.springframework.data.jpa.repository.query.JSqlParserUtils; // <- innecesario, puedes borrar

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Principal {

    private final ConsumoAPI consumoAPI = new ConsumoAPI();
    private final ConvierteDatos conversor = new ConvierteDatos();
    private static final String URL_BASE = "https://gutendex.com/books/";
    private final Scanner teclado = new Scanner(System.in);

    private final AutorRepository autorRepository;

    // Con un único constructor, @Autowired es implícito
    public Principal(AutorRepository repository) {
        this.autorRepository = repository; // <- CORRECTO
    }

    public void muestraMenu() {
        var opcion = 0;
        while (opcion != 9) {
            var menu = """
                    **************************************************
                                 Bienvenidos a LiterAlura
                              Busqueda de Libros y/o Autores
                    **************************************************

                    Selecciona la opción deseada:

                    1.- Buscar un libro por título.
                    2.- Buscar autor por nombre.
                    3.- Listar libros registrados.
                    4.- Listar autores registrados.
                    5.- Listar autores vivos en un determinado año.
                    6.- Listar libros por idioma.
                    7.- Estadísticas generales.
                    8.- Top 10 libros más descargados.
                    9.- Salir.
                    """;

            System.out.println(menu);
            try {
                opcion = teclado.nextInt();
                teclado.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Introduce un número válido.");
                teclado.nextLine();
                continue;
            }

            switch (opcion) {
                case 1 -> buscarLibroPorTitulo();
                case 2 -> buscarAutorPorNombre();
                case 3 -> listarLibrosRegistrados();
                case 4 -> listarAutoresRegistrados();
                case 5 -> listarAutoresVivos();
                case 6 -> listarLibrosPorIdioma();
                case 7 -> estadisticas();
                case 8 -> top10MasDescargados();
                case 9 -> System.out.println("Gracias por usar la aplicación, hasta pronto.\n");
                default -> System.out.println("Opción no válida");
            }
        }
    }

    // Opción 1
    private void buscarLibroPorTitulo() {
        System.out.println("Ingresa el título del libro que deseas buscar: ");
        var tituloLibro = teclado.nextLine();
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + tituloLibro.replace(" ", "+").toLowerCase());
        var datosBusqueda = conversor.obtenerDatos(json, Datos.class);

        if (datosBusqueda == null || datosBusqueda.resultados() == null) {
            System.out.println("Error al obtener datos de la API.");
            return;
        }

        Optional<DatosLibro> libroBuscado = datosBusqueda.resultados().stream()
                .filter(l -> l.titulo().toUpperCase().contains(tituloLibro.toUpperCase()))
                .findFirst();

        if (libroBuscado.isPresent()) {
            DatosLibro datosLibro = libroBuscado.get();
            System.out.println(
                    "Libro Encontrado : \n------------------------------" +
                            "\nTítulo: " + datosLibro.titulo() +
                            "\nAutor: " + datosLibro.autor().stream()
                            .map(DatosAutor::nombre).limit(1).collect(Collectors.joining()) +
                            "\nIdioma: " + String.join(",", datosLibro.idiomas()) +
                            "\nNúmero de descargas: " + datosLibro.numeroDeDescargas() +
                            "\n--------------------------------------\n");

            try {
                List<Libro> libroEncontrado = libroBuscado.stream()
                        .map(Libro::new)
                        .collect(Collectors.toList());

                Autor autorAPI = libroBuscado.stream()
                        .flatMap(l -> l.autor().stream().map(Autor::new))
                        .findFirst()
                        .orElse(null);

                Optional<Autor> autorBD = autorRepository.buscarAutorPorNombre(
                        datosLibro.autor().stream()
                                .map(DatosAutor::nombre)
                                .collect(Collectors.joining())
                );

                Optional<Libro> libroOptional = autorRepository.buscarLibroPorTitulo(datosLibro.titulo());

                if (libroOptional.isPresent()) {
                    System.out.println("El libro ya está guardado en la base de datos.");
                } else {
                    Autor autor;
                    if (autorBD.isPresent()) {
                        autor = autorBD.get();
                        System.out.println("El autor ya está guardado en la base de datos.");
                    } else {
                        autor = autorAPI;
                        autorRepository.save(autor);
                    }
                    autor.getLibros().addAll(libroEncontrado);
                    autorRepository.save(autor);
                }
            } catch (Exception e) {
                System.out.println("Error guardando en la base de datos: " + e.getMessage());
            }
        } else {
            System.out.println("Libro no encontrado");
        }
    }

    // Opción 2
    private void buscarAutorPorNombre() {
        System.out.println("Ingrese el nombre del autor que desea buscar: ");
        try {
            var autorBuscado = teclado.nextLine();
            Optional<Autor> autor = autorRepository.buscarAutorPorNombre(autorBuscado);
            if (autor.isPresent()) {
                System.out.println(autor.get());
            } else {
                System.out.println("Autor no encontrado.");
            }
        } catch (Exception e) {
            System.out.println("Error buscando autor: " + e.getMessage());
        }
    }

    // Opción 3
    private void listarLibrosRegistrados() {
        List<Libro> libros = autorRepository.librosRegistrados();
        libros.forEach(System.out::println);
    }

    // Opción 4
    private void listarAutoresRegistrados() {
        List<Autor> autores = autorRepository.findAll();
        autores.stream()
                .sorted(Comparator.comparing(Autor::getNombre))
                .forEach(System.out::println);
    }

    // Opción 5
    private void listarAutoresVivos() {
        System.out.println("Ingrese un año para verificar el autor(es) que desea buscar");
        try {
            var fecha = Integer.parseInt(teclado.nextLine());
            List<Autor> autores = autorRepository.listarAutoresVivos(fecha);
            if (!autores.isEmpty()) {
                autores.stream()
                        .sorted(Comparator.comparing(Autor::getNombre))
                        .forEach(System.out::println);
            } else {
                System.out.println("Ningún autor vivo encontrado en este año");
            }
        } catch (NumberFormatException e) {
            System.out.println("Ingrese un año válido: " + e.getMessage());
        }
    }

    // Opción 6
    private void listarLibrosPorIdioma() {
        var menuIdiomas = """
                Elija la opción deseada:

                1 - Español
                2 - Inglés
                """;
        System.out.println(menuIdiomas);

        try {
            var opcionIdioma = Integer.parseInt(teclado.nextLine());
            switch (opcionIdioma) {
                case 1 -> buscarLibrosPorIdioma("es");
                case 2 -> buscarLibrosPorIdioma("en");
                default -> System.out.println("Opción no válida");
            }
        } catch (NumberFormatException e) {
            System.out.println("Opción no válida: " + e.getMessage());
        }
    }

    private void buscarLibrosPorIdioma(String idioma) {
        try {
            Idioma idiomaEnum = Idioma.valueOf(idioma.toUpperCase());
            List<Libro> libros = autorRepository.librosPorIdioma(idiomaEnum);
            if (!libros.isEmpty()) {
                libros.forEach(System.out::println);
            } else {
                System.out.println("No hay libros registrados en ese idioma");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Introduce un idioma válido.");
        }
    }

    // Opción 7
    private void estadisticas() {
        var json = consumoAPI.obtenerDatos(URL_BASE);
        var datos = conversor.obtenerDatos(json, Datos.class);
        if (datos == null || datos.resultados() == null) {
            System.out.println("Error al obtener datos de la API.");
            return;
        }
        DoubleSummaryStatistics est = datos.resultados().stream()
                .filter(d -> d.numeroDeDescargas() > 0)
                .collect(Collectors.summarizingDouble(DatosLibro::numeroDeDescargas));

        System.out.println("Cantidad media de descargas: " + est.getAverage());
        System.out.println("Cantidad máxima de descargas: " + est.getMax());
        System.out.println("Cantidad mínima de descargas: " + est.getMin());
        System.out.println("Cantidad de registros evaluados: " + est.getCount());
    }

    // Opción 8
    private void top10MasDescargados() {
        var json = consumoAPI.obtenerDatos(URL_BASE);
        var datos = conversor.obtenerDatos(json, Datos.class);
        if (datos == null || datos.resultados() == null) {
            System.out.println("Error al obtener datos de la API.");
            return;
        }
        System.out.println("Top 10 libros más descargados: ");
        datos.resultados().stream()
                .sorted(Comparator.comparing(DatosLibro::numeroDeDescargas).reversed())
                .limit(10)
                .forEach(l -> System.out.println("[" + l.numeroDeDescargas() + " descargas] - " + l.titulo()));
    }
}
