# CarpoolApp — Aplicación de Viajes Compartidos para Medellín

---

## I. Introducción

El presente proyecto consiste en el desarrollo de **CarpoolApp**, una aplicación móvil Android de carpooling comunitario orientada a los habitantes de la ciudad de Medellín. La aplicación conecta a conductores que tienen asientos disponibles en su vehículo con pasajeros que se dirigen a un destino común, facilitando la coordinación de viajes compartidos de forma segura, confiable y en tiempo real. Implementada con Kotlin y Firebase (Firestore, Authentication, Cloud Messaging), la aplicación sigue una arquitectura MVVM con Clean Architecture, ofreciendo funcionalidades como publicación de viajes, búsqueda por destino, solicitud de cupo, gestión de solicitudes y notificaciones push.

La necesidad que motiva este proyecto surge de la problemática de movilidad que enfrenta la población de Medellín, particularmente en horario nocturno (6:00 p.m. a 10:00 p.m.), donde las opciones de transporte seguro y asequible son limitadas. El crecimiento poblacional, la congestión vehicular, los costos de transporte y la percepción de inseguridad generan una brecha entre las soluciones existentes (transporte público masivo, plataformas digitales) y las necesidades reales de los ciudadanos. Desarrollar CarpoolApp es importante porque ofrece una alternativa de movilidad colaborativa que reduce costos y tiempos de desplazamiento, mitiga riesgos de seguridad, disminuye la huella de carbono al reducir el número de vehículos en circulación y fomenta una cultura de cooperación entre los habitantes de la ciudad.

---

## II. Contextualización del problema

La necesidad histórica de asociación en la humanidad se basa en su naturaleza social y gregaria (Aristóteles, 1998), que impulsa a los seres humanos a agruparse para sobrevivir y prosperar de manera individual y colectiva. Esta condición innata, esencial para la evolución, se manifiesta en la formación de diversas estructuras sociales (Wilson, 2012), desde la familia hasta las ciudades y naciones, satisfaciendo necesidades como la seguridad, el afecto y la búsqueda de objetivos comunes.

El crecimiento poblacional contemporáneo ha dado lugar a la expansión de grandes metrópolis como Medellín, la capital del departamento de Antioquia en Colombia. Para el año 2024, la población total de Antioquia se estima en 6,903,721 habitantes (Gobernación de Antioquia, 2024).

**Figura 1.** Población censal en Antioquia 1985 - 2018

*Nota.* Figura recuperada del artículo "Informe Avance 2024" de la Gobernación de Antioquia, 2024, p. 4.

Dentro de este total, el municipio de Medellín, como principal centro urbano, alberga una población proyectada de 2,616,335 habitantes, una cifra que se espera que crezca hasta los 2,634,570 habitantes en 2025. Este crecimiento consolida la preponderancia de Medellín, que representa consistentemente el 37.9% de la población total de Antioquia (DEPARTAMENTO ADMINISTRATIVO NACIONAL DE ESTADÍSTICA - DANE, 2023).

Si bien este crecimiento demográfico es un signo de vitalidad, también genera nuevos y complejos retos urbanos. La concentración de una población significativa en un valle geográficamente limitado ejerce una presión considerable sobre la infraestructura, lo que se traduce en problemas sistémicos de movilidad y congestión vehicular, como lo menciona una nota periodística donde se habla del crecimiento del parque automotor en la ciudad de Medellín: "No hay vías suficientes para tantos vehículos" (Arbeláez Tabares, 2025). De igual manera, las proyecciones demográficas revelan cambios estructurales, como el envejecimiento de la población, el aumento de hogares unipersonales y la caída en la tasa de nacimientos, que demandan nuevas estrategias de planificación y desarrollo.

De la misma manera, el elevado volumen de vehículos que transitan diariamente por el Valle de Aburrá genera impactos ambientales significativos que agravan la problemática de movilidad en la región. Según el Inventario de Emisiones Atmosféricas del Valle de Aburrá - Fuentes Móviles del año 2022, elaborado por el Área Metropolitana del Valle de Aburrá (AMVA, 2023), las emisiones vehiculares constituyen una de las principales fuentes de contaminación atmosférica en la región metropolitana.

Las emisiones de Dióxido de Carbono (CO₂) provenientes tanto de vehículos privados como de transporte público se miden en gramos por kilómetro recorrido, considerando diferentes tipos de vehículos, tecnologías y combustibles. El inventario clasifica las emisiones según:

- Tipo de vehículo: automóviles particulares, motocicletas, buses, taxis y vehículos de carga.
- Combustible utilizado: gasolina, diésel, gas natural vehicular (GNV) y electricidad.
- Año del modelo: desde vehículos antiguos (pre-1990) hasta modelos recientes.

Es en este contexto de desafíos multifacéticos donde la movilidad urbana se convierte en un factor crítico para la calidad de vida de los habitantes de Medellín. La problemática de transporte en horario nocturno (6:00 p.m. a 10:00 p.m.) representa un obstáculo significativo para una amplia porción de la población que necesita opciones de desplazamiento seguras, confiables y económicas. La ausencia de estas alternativas genera múltiples consecuencias: un riesgo tangible para la seguridad personal, especialmente de mujeres y personas en situación de vulnerabilidad; la limitación de la capacidad para regresar al hogar de forma eficiente después de la jornada laboral o académica; y la reducción de oportunidades de desarrollo profesional, social y recreativo.

El objetivo principal de este documento es presentar una solución tecnológica a esta problemática, trascendiendo una visión reactiva que se limita a gestionar las consecuencias de la congestión y la inseguridad, para proponer un modelo de movilidad colaborativa que aproveche los recursos existentes (los asientos vacíos en vehículos particulares) y los ponga al servicio de la comunidad.

---

## III. Descripción del problema

Como antecedente al objeto de estudio, se tiene el diagrama de Ishikawa (2025) mediante el cual se identificaron las principales dificultades para acceder al transporte en horarios de alta demanda, visualizando circunstancias anexas como el costo del transporte por largos períodos de espera, la limitada frecuencia del servicio de transporte público y la inseguridad percibida. Esta última problemática incide negativamente tanto en el transporte público convencional como en alternativas de movilidad tales como motocicletas y plataformas digitales de transporte.

**Figura 2.** Diagrama de Ishikawa – Viajes compartidos comunitarios.

*Nota.* Figura de diseño propio.

A pesar de los esfuerzos realizados por las autoridades de la ciudad en términos de ofrecer servicios como el Metro, cables, buses y el sistema de transporte público integrado, estas soluciones no cubren todas las zonas de la ciudad con la frecuencia necesaria durante las horas más críticas de la noche. En consecuencia, se genera una brecha entre las soluciones existentes y la realidad de los ciudadanos de Medellín, dejando a una parte de la población en una situación de vulnerabilidad y sin opciones viables para su movilidad.

Teniendo en cuenta que la población económicamente activa de Medellín está compuesta por personas entre los 17 y 50 años que se desplazan diariamente por la ciudad, se realizó un sondeo de opinión dirigido a un grupo aleatorio de 20 ciudadanos (10 hombres y 10 mujeres), aplicando una encuesta con un cuestionario compuesto por 4 preguntas base:

**1. ¿Estaría dispuesto a utilizar o ser parte de una aplicación de tipo Uber Comunitario ofreciendo su vehículo?**

**Figura 3.** Disposición a usar una aplicación de viajes compartidos.

*Nota.* Figura de diseño propio.

**2. ¿Considera que compartir el vehículo con otra persona que se dirige al mismo sitio que usted tiene beneficios?**

**Figura 4.** Compartir vehículo trae beneficios.

*Nota.* Figura de diseño propio.

**3. ¿Piensa usted que una plataforma de viajes compartidos mitigaría positivamente los riesgos de seguridad y vulnerabilidad en horas de la noche (Merchán Núñez, 2025), luego de terminar la jornada laboral o académica?**

**Figura 5.** Mitigación de riesgo de seguridad en la noche.

*Nota.* Figura de diseño propio.

**4. ¿Ha utilizado alguna vez una aplicación o plataforma de viajes compartidos (carpooling)?**

**Figura 6.** Uso previo de plataformas de viajes compartidos.

*Nota.* Figura de diseño propio.

---

## IV. Objetivos

### IV.I Objetivo general

Optimizar la eficiencia del transporte y la compensación de costos de movilidad en la ciudad de Medellín a través de una plataforma de viajes compartidos que promueva una movilidad sostenible, segura y colaborativa entre sus habitantes.

### IV.II Objetivos específicos

1. **Analizar** las principales necesidades de movilidad de los ciudadanos de Medellín en horario nocturno, identificando factores de riesgo y variables de influencia en los viajes compartidos, con el fin de establecer los requisitos funcionales y no funcionales de la aplicación CarpoolApp.

2. **Diseñar** una plataforma digital que permita a los habitantes de Medellín acceder de manera fácil y segura al servicio de transporte compartido, reduciendo los tiempos de espera y mejorando la calidad de vida mediante una interfaz intuitiva y un flujo de solicitud-confianza eficiente.

3. **Desarrollar** una aplicación móvil que fomente la interacción y el sentido de comunidad entre los usuarios, promoviendo la colaboración y el respeto mutuo, e implementar un plan de aseguramiento de calidad que garantice la confiabilidad, seguridad y usabilidad del producto de software.

---

## V. Justificación

Existe una dificultad significativa en la población de Medellín para conseguir transporte seguro y confiable en horario nocturno, pese a las diferentes opciones que ofrece el sistema de transporte público integrado (Metro, cables, buses). La aplicación móvil de transporte compartido se alinea con el concepto de "ride-sharing" o "viaje compartido" o "carpooling", que busca "promover el transporte sostenible, reducir la utilización del automóvil, aumentar la ocupación de los vehículos y la afluencia de pasajeros al transporte público" (Mitropoulos et al., 2021), y ofrecer una alternativa diferente a los modelos comerciales. Esta propuesta tiene un enfoque sin ánimo de lucro, donde el conductor recibe una compensación voluntaria por parte de los pasajeros, sin tarifas fijas ni comisiones, fomentando la cooperación y la confianza entre los usuarios.

El proyecto es relevante porque considera las problemáticas socioeconómicas de los ciudadanos para desarrollar una plataforma que ofrece soluciones innovadoras. En particular, el modelo de compensación propuesto —basado en la contribución voluntaria y opcional del pasajero— elimina la barrera económica directa y reduce la incertidumbre en las transacciones monetarias. Este modelo de colaboración fomenta la participación de los usuarios y se alinea con estrategias de economía colaborativa que han demostrado ser efectivas en programas de fidelización y recompensas para atraer y retener usuarios.

Un aspecto clave para la adopción de este tipo de servicios es la seguridad y la confianza entre usuarios, según Merchán Núñez (2025). Un aspecto clave para garantizar la seguridad y poder mejorar la calidad del servicio es la retroalimentación de los usuarios. Todas las plataformas permiten que se realicen comentarios para mantener estándares de calidad y fomentar la confianza.

La revisión de la literatura destaca que la mayoría de las plataformas de transporte compartido se han desarrollado a partir de 2010, coincidiendo con la proliferación de los teléfonos inteligentes (Mitropoulos et al., 2021). Un servicio de transporte que ofrezca reservas en tiempo real y sea fácil de usar a través de una aplicación móvil es crucial para el éxito y para minimizar las barreras de tiempo para los usuarios.

El uso de este tipo de aplicaciones se concentra principalmente en Estados Unidos, la Unión Europea y Asia. La Figura 7 muestra el uso y la adopción del carpooling en distintas regiones del mundo (Merchán Núñez, 2025). Analizando estos datos se puede concluir que en Colombia esta adopción no es muy frecuente, lo que hace posible promover su uso entre los ciudadanos de Medellín y así reducir la huella de carbono que se genera con el uso de vehículos particulares.

**Figura 7.** Adopción del vehículo compartido por región.

*Nota.* Figura recuperada de Merchán Núñez (2025).

Las áreas de investigación futura deben centrarse en "el propósito de los viajes de los usuarios de transporte compartido (es decir, trabajo, universidad, compras, entre otras)" (Mitropoulos et al., 2021), validando así la relevancia de un proyecto enfocado en la movilidad urbana. La aplicación no solo resolverá un problema práctico, sino que también servirá como un estudio de caso para evaluar factores de adopción, comportamiento del usuario y la efectividad de un modelo de compensación no tradicional en un entorno urbano.

Hay un factor clave en la adopción de esta propuesta y es la oportunidad de ayudar a las personas de pocos recursos económicos, que destinan una parte significativa de sus ingresos al transporte diario. Los viajes compartidos representan una alternativa concreta para reducir este gasto, y hoy instituciones como el ITM ya están fomentando esta práctica, aunque sin una aplicación que conecte directamente a los dueños de vehículos con los usuarios que lo necesitan.

**Figura 8.** Fomento del uso de carpooling — ejemplo de la comunidad universitaria del ITM.

*Nota.* Imagen tomada de la página institucional del ITM. Se incluye como referencia de una iniciativa existente de promoción de viajes compartidos en el ámbito local.

Con base en todo lo expuesto, lo que se pretende con este proyecto de grado de la Especialización en Ingeniería de Software es brindar nuevas opciones válidas para una generación de modalidad de transporte, donde se tengan en cuenta las potencialidades de conductores particulares para contribuir al mejoramiento de las condiciones de movilidad de los demás ciudadanos en los horarios críticos, como son las horas nocturnas, y donde Medellín puede marcar un precedente en la adopción de modelos de movilidad colaborativa en Colombia.

Como profesionales especializados, contribuimos a que los escenarios urbanos beneficien a sus habitantes con alternativas viables que garanticen desplazamientos efectivos, disminuyendo costos económicos, empleando opciones motivadoras de servicio compartido y apostando por cambios en estructuras establecidas con enfoques comunitarios.

El proyecto contribuye a visualizar opciones que solucionan necesidades sentidas de movilidad en la ciudad de Medellín, como es el transporte de manera eficiente y segura, fomentando la movilidad sostenible, reduciendo la congestión vehicular y, por ende, la contaminación.

---

*Documento generado como parte del proyecto de desarrollo de CarpoolApp.*
