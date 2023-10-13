UPDATE artist
SET albums = (SELECT jsonb_agg(
                             jsonb_set(
                                     album,
                                     '{songs}',
                                     (SELECT jsonb_agg(jsonb_set(song - 'comment', '{}', 'null'))
                                      FROM jsonb_array_elements(album -> 'songs') AS song)
                                 )
                         )
              FROM jsonb_array_elements(albums) AS album);