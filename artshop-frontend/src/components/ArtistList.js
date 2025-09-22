import React, { useState, useEffect } from 'react';
import {
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    Paper, TextField, Button, Box, Typography
} from '@mui/material';
import { Link } from 'react-router-dom';
import axios from 'axios';

const ArtistList = () => {
    const [artists, setArtists] = useState([]);
    const [searchFirstName, setSearchFirstName] = useState('');
    const [searchLastName, setSearchLastName] = useState('');

    useEffect(() => {
        fetchArtists();
    }, []);

    const fetchArtists = async () => {
        try {
            const response = await axios.get('http://localhost:8100/api/artist/all');
            setArtists(response.data);
        } catch (error) {
            console.error('Error fetching artists:', error);
        }
    };

    const searchArtists = async () => {
        try {
            let url = 'http://localhost:8100/api/artist/name?';
            if (searchFirstName) url += `firstName=${searchFirstName}&`;
            if (searchLastName) url += `lastName=${searchLastName}`;

            const response = await axios.get(url);
            setArtists(response.data);
        } catch (error) {
            console.error('Error searching artists:', error);
        }
    };

    const deleteArtist = async (id) => {
        if (window.confirm('Вы уверены, что хотите удалить этого артиста?')) {
            try {
                await axios.delete(`http://localhost:8100/api/artist/${id}`);
                fetchArtists();
            } catch (error) {
                console.error('Error deleting artist:', error);
            }
        }
    };

    return (
        <div>
            <Typography variant="h4" gutterBottom>
                Список артистов
            </Typography>

            <Box sx={{
                mb: 3,
                display: 'flex',
                gap: 2,
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%'
            }}>
                <TextField
                    label="Имя"
                    value={searchFirstName}
                    onChange={(e) => setSearchFirstName(e.target.value)}
                    size="small"
                />
                <TextField
                    label="Фамилия"
                    value={searchLastName}
                    onChange={(e) => setSearchLastName(e.target.value)}
                    size="small"
                />
                <Button variant="contained" onClick={searchArtists}>
                    ПОИСК
                </Button>
            </Box>

            <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3 }}>
                <Button
                    variant="contained"
                    color="primary"
                    component={Link}
                    to="/artists/add"
                >
                    ДОБАВИТЬ АРТИСТА
                </Button>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>№</TableCell>
                            <TableCell>Имя</TableCell>
                            <TableCell>Отчество</TableCell>
                            <TableCell>Фамилия</TableCell>
                            <TableCell>Произведения</TableCell>
                            <TableCell>Действия</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {artists.map((artist, index) => (
                            <TableRow key={artist.id}>
                                <TableCell>{index + 1}</TableCell>
                                <TableCell>{artist.firstName || '-'}</TableCell>
                                <TableCell>{artist.middleName || '-'}</TableCell>
                                <TableCell>{artist.lastName || '-'}</TableCell>
                                <TableCell>
                                    {artist.artworkCount > 0 ? (
                                        artist.artworkTitles.join(', ')
                                    ) : (
                                        'Нет произведений'
                                    )}
                                </TableCell>
                                <TableCell>
                                    <Button
                                        component={Link}
                                        to={`/artists/edit/${artist.id}`}
                                        variant="outlined"
                                        size="small"
                                        sx={{ mr: 1 }}
                                    >
                                        РЕДАКТИРОВАТЬ
                                    </Button>
                                    <Button
                                        variant="outlined"
                                        color="error"
                                        size="small"
                                        onClick={() => deleteArtist(artist.id)}
                                    >
                                        УДАЛИТЬ
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    );
};

export default ArtistList;